package com.eyram.dev.church_project_spring.service.payment.saspay;

import com.eyram.dev.church_project_spring.DTO.response.*;
import com.eyram.dev.church_project_spring.config.*;
import com.eyram.dev.church_project_spring.entities.*;
import com.eyram.dev.church_project_spring.enums.*;
import com.eyram.dev.church_project_spring.repositories.*;
import com.eyram.dev.church_project_spring.service.DemandePaymentEligibilityService;
import com.eyram.dev.church_project_spring.service.accounting.ParishLedgerService;
import com.eyram.dev.church_project_spring.service.billing.SubscriptionBillingService;
import com.eyram.dev.church_project_spring.service.payment.PaymentReturnTokenService;
import com.eyram.dev.church_project_spring.utils.BusinessCodeGenerator;
import com.eyram.dev.church_project_spring.utils.exception.*;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@lombok.extern.slf4j.Slf4j
@Service
@RequiredArgsConstructor
public class SasPayPaymentService {
    private final SasPayClient client;
    private final SasPayCheckoutStore store;
    private final SasPayProperties properties;
    private final PlatformBillingProperties platform;
    private final DemandeRepository demands;
    private final FactureRepository invoices;
    private final DetailsPaiementRepository payments;
    private final ParoisseAbonnementRepository subscriptions;
    private final ParoisseRepository parishes;
    private final DemandePaymentEligibilityService eligibility;
    private final PaymentReturnTokenService tokens;
    private final ParishLedgerService ledger;
    private final SubscriptionBillingService billing;
    private final TransactionTemplate transactions;

    public PaymentFeeBreakdown quote(String code) {
        return transactions.execute(
                t -> {
                    var invoice = invoice(code);
                    var d = invoice.getDemande();
                    return fees(invoice, d.getTypePaiement().getMode());
                });
    }

    private PaymentFeeBreakdown fees(Facture invoice, ModePaiement mode) {
        int amount = invoice.getMontant();
        int service =
                mode == ModePaiement.ESPECES
                        ? 0
                        : BigDecimal.valueOf(amount)
                                        .multiply(
                                                platform.getCommissionPercent()
                                                        .max(BigDecimal.ZERO))
                                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                                        .intValueExact()
                                + Math.max(0, platform.getCommissionFixedXof());
        // SasPay chooses the real network on its hosted page: never reuse FedaPay fee estimates.
        return new PaymentFeeBreakdown(
                mode,
                FeePayer.CUSTOMER,
                mode == ModePaiement.ESPECES ? BigDecimal.ZERO : null,
                platform.getCommissionPercent(),
                0,
                platform.getCommissionFixedXof(),
                amount,
                0,
                service,
                service,
                Math.addExact(amount, service),
                amount,
                service);
    }

    public PaymentCheckoutResponse checkout(String code, String payerEmail) {
        var current =
                transactions.execute(t -> store.latest("DEMANDE", invoice(code).getPublicId()));
        if (current != null && "PENDING".equals(current.state())) refresh(current);
        var checkout =
                transactions.execute(
                        t -> {
                            var demandId =
                                    demands.findPublicIdByTrackingCode(
                                                    BusinessCodeGenerator
                                                            .normalizeDemandeTrackingCode(code))
                                            .orElseThrow(
                                                    () ->
                                                            new TrackingIdNotFoundException(
                                                                    "Code de suivi introuvable"));
                            demands.findForCashByPublicId(demandId).orElseThrow();
                            var found =
                                    invoices.findByDemandePublicIdAndStatusDelFalse(demandId)
                                            .orElseThrow();
                            var invoice =
                                    invoices.findByPublicIdForUpdate(found.getPublicId())
                                            .orElseThrow();
                            var d = invoice.getDemande();
                            if (invoice.getStatutPaiement() == StatutPaiementEnum.PAYE
                                    || d.getStatutPaiement() == StatutPaiementEnum.PAYE)
                                return null;
                            eligibility.assertCanStartPayment(d);
                            var fees = fees(invoice, d.getTypePaiement().getMode());
                            if (fees.mode() == ModePaiement.ESPECES) return null;
                            var prior = store.latest("DEMANDE", invoice.getPublicId());
                            if (prior != null
                                    && Set.of("PENDING", "UNCERTAIN", "REVIEW")
                                            .contains(prior.state())) {
                                if (prior.amount() != fees.montantCharge())
                                    throw new BusinessRuleException(
                                            "Une session ouverte porte un autre montant. Vérifiez"
                                                    + " son statut.");
                                return prior;
                            }
                            client.requireConfigured();
                            String email =
                                    payerEmail != null && !payerEmail.isBlank()
                                            ? payerEmail.strip()
                                            : d.getEmailFidele();
                            if (email == null || email.isBlank())
                                throw new BusinessRuleException(
                                        "Renseignez une adresse e-mail pour recevoir la"
                                                + " confirmation de paiement SasPay.");
                            var body =
                                    body(
                                            fees.montantCharge(),
                                            "Demande de messe",
                                            email,
                                            d.getPrenomFidele() + " " + d.getNomFidele(),
                                            d.getTelFidele(),
                                            properties.getAppUrl().replaceAll("/+$", "")
                                                    + "/paiement/retour?r="
                                                    + tokens.issue(d.getCodeSuivie()));
                            var session =
                                    store.start(
                                            "DEMANDE",
                                            invoice.getPublicId(),
                                            fees.montantCharge(),
                                            fees.montantFraisPlateforme(),
                                            body);
                            if (session.amount() != fees.montantCharge())
                                throw new BusinessRuleException(
                                        "Une session existe pour un autre montant. Vérifiez-la"
                                                + " avant un nouveau paiement.");
                            if ("PENDING".equals(session.state())) {
                                var detail =
                                        payments.findByFacturePublicId(invoice.getPublicId())
                                                .orElseGet(DetailsPaiement::new);
                                detail.setFacture(invoice);
                                detail.setTypePaiement(d.getTypePaiement());
                                detail.setNumero(d.getTelFidele());
                                detail.setStatusDel(false);
                                detail.setDateDetailsPaiement(LocalDateTime.now());
                                detail.setMontant(invoice.getMontant());
                                detail.setProvider("SASPAY");
                                detail.setIdTransaction(session.reference());
                                detail.setPaymentUrl(session.url());
                                detail.setMontantFraisPlateforme(fees.montantFraisPlateforme());
                                detail.setMontantFraisAgregeateur(0);
                                detail.setMontantFrais(fees.montantFraisPlateforme());
                                detail.setMontantCharge(fees.montantCharge());
                                detail.setMontantNet(invoice.getMontant());
                                detail.setStatutPaiement(StatutPaiementEnum.EN_ATTENTE);
                                payments.save(detail);
                                sync(invoice, StatutPaiementEnum.EN_ATTENTE, null);
                            }
                            return session;
                        });
        if (checkout != null && !Set.of("PENDING", "SUCCESS").contains(checkout.state()))
            throw new BusinessRuleException(
                    checkout.error() != null
                            ? checkout.error()
                            : "Cette session nécessite une vérification avant un nouveau"
                                    + " paiement.");
        return snapshot(code);
    }

    public Map<String, Object> body(
            int amount,
            String description,
            String email,
            String name,
            String phone,
            String returnUrl) {
        var body = new LinkedHashMap<String, Object>();
        body.put("amount", BigDecimal.valueOf(amount).setScale(2).toPlainString());
        body.put("currency", "XOF");
        body.put("country", properties.getCountry());
        body.put("description", description);
        body.put("customer_email", email);
        body.put("customer_name", name);
        if (phone != null && !phone.isBlank()) body.put("customer_phone", phone);
        body.put("fee_charge_mode", "ADD_ON");
        body.put("return_url", returnUrl);
        body.put("expires_at", java.time.Instant.now().plusSeconds(3300).toString());
        return body;
    }

    public PaymentCheckoutResponse reconcile(String code) {
        var c = transactions.execute(t -> store.latest("DEMANDE", invoice(code).getPublicId()));
        if (c != null && Set.of("PENDING", "UNCERTAIN").contains(c.state()) && c.remoteId() != null)
            refresh(c);
        return snapshot(code);
    }

    public PaymentCheckoutResponse snapshot(String code) {
        return transactions.execute(
                t -> {
                    var invoice = invoice(code);
                    var d = invoice.getDemande();
                    var q = fees(invoice, d.getTypePaiement().getMode());
                    var detail =
                            payments.findByFacturePublicIdAndStatusDelFalse(invoice.getPublicId())
                                    .orElse(null);
                    boolean paid = invoice.getStatutPaiement() == StatutPaiementEnum.PAYE;
                    boolean online =
                            !paid
                                    && detail != null
                                    && "SASPAY".equals(detail.getProvider())
                                    && detail.getStatutPaiement() == StatutPaiementEnum.EN_ATTENTE;
                    return new PaymentCheckoutResponse(
                            d.getCodeSuivie(),
                            d.getStatutPaiement(),
                            q.mode(),
                            online,
                            online ? detail.getPaymentUrl() : null,
                            detail == null ? null : detail.getIdTransaction(),
                            q.montantFacture(),
                            paid && detail != null ? detail.getMontantFraisAgregeateur() : 0,
                            q.montantFraisPlateforme(),
                            paid && detail != null ? detail.getMontantFrais() : q.montantFrais(),
                            paid && detail != null ? detail.getMontantCharge() : q.montantCharge(),
                            q.montantNetParoisse(),
                            q.montantNetPlateforme(),
                            paid ? BigDecimal.ZERO : null,
                            q.feePercentPlateforme(),
                            "CUSTOMER",
                            paid
                                    ? "Paiement confirmé."
                                    : "Les frais SasPay et le total définitif sont affichés sur la"
                                            + " page de paiement avant validation.");
                });
    }

    private Facture invoice(String code) {
        String normalized = BusinessCodeGenerator.normalizeDemandeTrackingCode(code);
        var d =
                demands.findByCodeSuivieWithAssociations(normalized)
                        .orElseThrow(
                                () -> new TrackingIdNotFoundException("Code de suivi introuvable"));
        return invoices.findByDemandePublicIdAndStatusDelFalse(d.getPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
    }

    public void refresh(SasPayCheckoutStore.Checkout checkout) {
        store.touch(checkout.id());
        var status = client.status(checkout.remoteId());
        if (!checkout.remoteId().equals(status.path("id").asText()))
            throw new BusinessRuleException(
                    "La session SasPay ne correspond pas au paiement attendu.");
        String state = status.path("status").asText();
        String transaction = status.path("transaction_id").asText("");
        if ("PAID".equals(state) || "SUCCESS".equals(status.path("transaction_status").asText())) {
            UUID.fromString(transaction);
            var verified = client.verify(transaction);
            if (!transaction.equals(verified.path("id").asText()))
                throw new BusinessRuleException("Référence SasPay incohérente.");
            confirm(checkout, verified);
        } else if (Set.of("EXPIRED", "CANCELLED").contains(state)
                || "FAILED".equals(status.path("transaction_status").asText())) {
            transactions.executeWithoutResult(
                    t -> {
                        if ("DEMANDE".equals(checkout.purpose())) lockInvoice(checkout.target());
                        var locked = store.get(checkout.id(), true);
                        if ("SUCCESS".equals(locked.state())) return;
                        store.state(locked.id(), "FAILED");
                        if ("DEMANDE".equals(locked.purpose())) {
                            var invoice =
                                    invoices.findByPublicIdForUpdate(locked.target()).orElseThrow();
                            var detail =
                                    payments.findByFacturePublicIdAndStatusDelFalse(
                                                    invoice.getPublicId())
                                            .orElse(null);
                            if (detail != null
                                    && locked.reference().equals(detail.getIdTransaction())
                                    && detail.getStatutPaiement() != StatutPaiementEnum.PAYE) {
                                detail.setStatutPaiement(StatutPaiementEnum.ECHOUE);
                                payments.save(detail);
                                sync(invoice, StatutPaiementEnum.ECHOUE, null);
                            }
                        }
                    });
        }
    }

    public void confirm(SasPayCheckoutStore.Checkout expected, JsonNode payment) {
        if (!"SUCCESS".equals(payment.path("status").asText())) return;
        String id = UUID.fromString(payment.path("id").asText()).toString();
        if (!"XOF".equals(payment.path("currency").asText())
                || !"PAIEMENT".equals(payment.path("transaction_type").asText())
                || !"INBOUND".equals(payment.path("flow_direction").asText()))
            throw new BusinessRuleException("Paiement SasPay de nature ou devise incorrecte.");
        var amount = decimal(payment, "requested_amount");
        var charged = decimal(payment, "debited_amount");
        var net = decimal(payment, "net_amount");
        var providerFee = decimal(payment, "client_fee");
        if (amount.compareTo(BigDecimal.valueOf(expected.amount())) != 0
                || net.compareTo(amount) != 0
                || charged.compareTo(amount.add(providerFee)) != 0
                || !"ADD_ON".equals(payment.path("fee_charge_mode").asText())) {
            transactions.executeWithoutResult(t -> store.state(expected.id(), "REVIEW"));
            return;
        }
        transactions.executeWithoutResult(
                t -> {
                    // Shared ordering with checkout/cash: demand -> invoice -> provider row.
                    // Subscription activation uses parish -> subscription -> provider row.
                    if ("DEMANDE".equals(expected.purpose())) lockInvoice(expected.target());
                    else {
                        parishes.findByPublicIdForUpdate(
                                        subscriptions.findParishId(expected.target()).orElseThrow())
                                .orElseThrow();
                        subscriptions.findByPublicIdForUpdate(expected.target()).orElseThrow();
                    }
                    var locked = store.get(expected.id(), true);
                    if ("SUCCESS".equals(locked.state())) return;
                    if ("DEMANDE".equals(locked.purpose())) {
                        var invoice =
                                invoices.findByPublicIdForUpdate(locked.target()).orElseThrow();
                        var d = invoice.getDemande();
                        var detail =
                                payments.findByFacturePublicIdAndStatusDelFalse(
                                                invoice.getPublicId())
                                        .orElse(null);
                        if (detail == null
                                || !locked.reference().equals(detail.getIdTransaction())
                                || invoice.getMontant() != locked.amount() - locked.platformFee()
                                || invoice.getStatutPaiement() == StatutPaiementEnum.PAYE
                                || d.getTypePaiement().getMode() == ModePaiement.ESPECES) {
                            store.state(locked.id(), "REVIEW");
                            return;
                        }
                        detail.setProviderCharged(charged);
                        detail.setProviderNet(net);
                        detail.setProviderFee(providerFee);
                        detail.setMontantCharge(rounded(charged));
                        detail.setMontantFraisAgregeateur(rounded(providerFee));
                        detail.setMontantFrais(
                                rounded(providerFee.add(BigDecimal.valueOf(locked.platformFee()))));
                        detail.setMontantNet(invoice.getMontant());
                        detail.setStatutPaiement(StatutPaiementEnum.PAYE);
                        detail.setDateDetailsPaiement(LocalDateTime.now());
                        payments.save(detail);
                        sync(invoice, StatutPaiementEnum.PAYE, LocalDateTime.now());
                        ledger.creditMesse(
                                d.getParoisse(),
                                invoice.getMontant(),
                                d.getCodeSuivie(),
                                "SASPAY:" + id);
                    } else {
                        var subscription =
                                subscriptions
                                        .findByPublicIdAndStatusDelFalse(locked.target())
                                        .orElseThrow();
                        if (subscription.getMontant() != locked.amount()
                                || !locked.reference().equals(subscription.getIdTransaction())) {
                            store.state(locked.id(), "REVIEW");
                            return;
                        }
                        billing.activateFromProviderTransaction(locked.reference());
                    }
                    store.paid(locked.id(), id, charged, net, providerFee);
                });
    }

    private Facture lockInvoice(UUID id) {
        UUID demandId = invoices.findDemandId(id).orElseThrow();
        demands.findForCashByPublicId(demandId).orElseThrow();
        return invoices.findByPublicIdForUpdate(id).orElseThrow();
    }

    private static BigDecimal decimal(JsonNode node, String key) {
        try {
            var value = new BigDecimal(node.path(key).asText());
            if (value.signum() < 0
                    || value.scale() > 2
                    || value.compareTo(new BigDecimal("99999999.99")) > 0)
                throw new IllegalArgumentException();
            return value;
        } catch (Exception e) {
            throw new BusinessRuleException("Montants SasPay incomplets ou invalides.");
        }
    }

    private static int rounded(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private void sync(Facture invoice, StatutPaiementEnum state, LocalDateTime at) {
        invoice.setStatutPaiement(state);
        invoice.setDatePaiement(at);
        invoices.save(invoice);
        var d = invoice.getDemande();
        d.setStatutPaiement(state);
        demands.save(d);
    }

    @Scheduled(fixedDelayString = "${saspay.poll-delay-ms:10000}", scheduler = "saspayScheduler")
    public void poll() {
        if (!client.configured()) return;
        for (var checkout : store.pending()) {
            try {
                refresh(checkout);
            } catch (Exception unavailable) {
                log.warn("Rapprochement SasPay reporté pour la session {}", checkout.id());
            }
        }
    }
}
