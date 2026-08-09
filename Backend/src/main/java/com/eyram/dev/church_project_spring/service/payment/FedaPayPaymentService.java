package com.eyram.dev.church_project_spring.service.payment;

import com.eyram.dev.church_project_spring.DTO.response.PaymentCheckoutResponse;
import com.eyram.dev.church_project_spring.DTO.response.PaymentFeeBreakdown;
import com.eyram.dev.church_project_spring.config.FedaPayProperties;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import com.eyram.dev.church_project_spring.entities.Facture;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.DetailsPaiementRepository;
import com.eyram.dev.church_project_spring.repositories.FactureRepository;
import com.eyram.dev.church_project_spring.service.accounting.ParishLedgerService;
import com.eyram.dev.church_project_spring.service.billing.SubscriptionBillingService;
import com.eyram.dev.church_project_spring.service.payment.fedapay.FedaPayClient;
import com.eyram.dev.church_project_spring.service.payment.fedapay.FedaPayPhone;
import com.eyram.dev.church_project_spring.service.payment.fedapay.FedaPayWebhookVerifier;
import com.eyram.dev.church_project_spring.utils.BusinessCodeGenerator;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import com.eyram.dev.church_project_spring.utils.exception.TrackingIdNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FedaPayPaymentService {

    public static final String PROVIDER_FEDAPAY = "FEDAPAY";
    public static final String PROVIDER_MANUAL = "MANUAL";
    /** Espèces reçues au secrétariat : hors solde de reversement plateforme. */
    public static final String PROVIDER_CAISSE_LOCALE = "CAISSE_LOCALE";

    private final DemandeRepository demandeRepository;
    private final FactureRepository factureRepository;
    private final DetailsPaiementRepository detailsPaiementRepository;
    private final PaymentFeeCalculator feeCalculator;
    private final PaymentReturnTokenService paymentReturnTokenService;
    private final FedaPayClient fedaPayClient;
    private final FedaPayWebhookVerifier webhookVerifier;
    private final FedaPayProperties properties;
    private final ObjectMapper objectMapper;
    private final ParishLedgerService parishLedgerService;
    private final SubscriptionBillingService subscriptionBillingService;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public PaymentFeeBreakdown quoteByTrackingCode(String codeSuivie) {
        Demande demande = requireDemande(codeSuivie);
        Facture facture = requireFacture(demande);
        ModePaiement mode = resolveMode(demande);
        return feeCalculator.calculate(facture.getMontant(), mode);
    }

    /**
     * Flux FedaPay conforme :
     * 1) préparer / réutiliser une session
     * 2) appels API hors transaction (create + token)
     * 3) persister l'URL
     * Confirmation métier = webhook {@code transaction.approved} (ou réconciliation GET).
     */
    public PaymentCheckoutResponse checkout(String codeSuivie) {
        CheckoutPrep prep = transactionTemplate.execute(status -> prepareCheckout(codeSuivie));
        if (prep == null) {
            throw new BusinessRuleException("Impossible de préparer le paiement");
        }
        if (prep.earlyResponse() != null) {
            return prep.earlyResponse();
        }

        // Session existante : le statut réel est chez FedaPay (GET), pas en local seul.
        if (StringUtils.hasText(prep.reuseTransactionId())) {
            final String existingTxId = prep.reuseTransactionId();
            final String existingPaymentUrl = prep.reusePaymentUrl();
            String remoteStatus = fetchRemoteStatus(existingTxId);
            if (isApprovedStatus(remoteStatus)) {
                transactionTemplate.executeWithoutResult(status -> markPaid(existingTxId));
                return quoteCheckoutSnapshot(codeSuivie, "Paiement déjà confirmé chez FedaPay.");
            }
            if (isFailedStatus(remoteStatus)) {
                transactionTemplate.executeWithoutResult(status -> markFailed(existingTxId));
                // Nouvelle transaction ci-dessous (prepareCheckout ne réutilise plus EN_ATTENTE).
                CheckoutPrep afterFail = transactionTemplate.execute(status -> prepareCheckout(codeSuivie));
                if (afterFail == null) {
                    throw new BusinessRuleException("Impossible de préparer le paiement");
                }
                if (afterFail.earlyResponse() != null) {
                    return afterFail.earlyResponse();
                }
                prep = afterFail;
            } else {
                // pending / unknown : réutiliser l'URL ou régénérer le token (pas de double create).
                if (StringUtils.hasText(existingPaymentUrl)) {
                    return quoteCheckoutSnapshot(codeSuivie, "Session de paiement déjà ouverte.");
                }
                long existingId = Long.parseLong(existingTxId.trim());
                FedaPayClient.PaymentToken token = fedaPayClient.generateToken(existingId);
                CheckoutPrep reusePrep = prep;
                return transactionTemplate.execute(status ->
                        persistReusedToken(reusePrep, existingId, token)
                );
            }
        }

        if (prep.customer() == null || prep.fees() == null) {
            throw new BusinessRuleException("Préparation paiement incomplète");
        }

        FedaPayClient.CreatedTransaction created = fedaPayClient.createTransaction(
                new FedaPayClient.CreateTransactionCommand(
                        "Demande de messe " + prep.codeSuivie(),
                        prep.fees().montantCharge(),
                        prep.callbackUrl(),
                        prep.customer(),
                        prep.metadata()
                )
        );
        FedaPayClient.PaymentToken token = fedaPayClient.generateToken(created.id());

        CheckoutPrep createPrep = prep;
        return transactionTemplate.execute(status ->
                persistCheckout(createPrep, created, token)
        );
    }

    /**
     * Après callback_url FedaPay : résout le jeton puis synchronise le statut via GET /transactions/:id.
     * Ne fait pas confiance seule au query param {@code status}.
     */
    public Map<String, String> resolveReturnAndReconcile(String returnToken, String providerTransactionId) {
        String code = paymentReturnTokenService.resolve(returnToken);
        reconcileByTrackingCode(code, providerTransactionId);
        Demande demande = requireDemande(code);
        Map<String, String> out = new LinkedHashMap<>();
        out.put("codeSuivie", demande.getCodeSuivie());
        out.put("statutPaiement", demande.getStatutPaiement() != null
                ? demande.getStatutPaiement().name()
                : "");
        return out;
    }

    /**
     * Réconciliation explicite (retour navigateur ou polling).
     */
    public PaymentCheckoutResponse reconcileByTrackingCode(String codeSuivie, String providerTransactionIdHint) {
        DetailsPaiement details = transactionTemplate.execute(status -> {
            Demande demande = requireDemande(codeSuivie);
            Facture facture = requireFacture(demande);
            return detailsPaiementRepository
                    .findByFacturePublicIdAndStatusDelFalse(facture.getPublicId())
                    .orElse(null);
        });

        String txId = StringUtils.hasText(providerTransactionIdHint)
                ? providerTransactionIdHint.trim()
                : (details != null ? details.getIdTransaction() : null);

        if (StringUtils.hasText(txId)) {
            String remoteStatus = fetchRemoteStatus(txId);
            if (isApprovedStatus(remoteStatus)) {
                transactionTemplate.executeWithoutResult(status -> markPaid(txId));
            } else if (isFailedStatus(remoteStatus)) {
                transactionTemplate.executeWithoutResult(status -> markFailed(txId));
            }
        }

        return quoteCheckoutSnapshot(codeSuivie, "Statut synchronisé avec FedaPay.");
    }

    private CheckoutPrep prepareCheckout(String codeSuivie) {
        Demande demande = requireDemande(codeSuivie);
        Facture facture = requireFacture(demande);
        ModePaiement mode = resolveMode(demande);
        PaymentFeeBreakdown fees = feeCalculator.calculate(facture.getMontant(), mode);

        if (mode == ModePaiement.ESPECES) {
            return CheckoutPrep.done(checkoutResponse(
                    demande, mode, fees, false, null, null,
                    "Paiement au comptant en paroisse — aucun paiement en ligne."
            ));
        }

        if (demande.getStatutPaiement() == StatutPaiementEnum.PAYE
                || facture.getStatutPaiement() == StatutPaiementEnum.PAYE) {
            DetailsPaiement existing = detailsPaiementRepository
                    .findByFacturePublicIdAndStatusDelFalse(facture.getPublicId())
                    .orElse(null);
            return CheckoutPrep.done(checkoutResponse(
                    demande, mode, fees, false,
                    existing != null ? existing.getPaymentUrl() : null,
                    existing != null ? existing.getIdTransaction() : null,
                    "Paiement déjà confirmé."
            ));
        }

        Optional<DetailsPaiement> existingOpt = detailsPaiementRepository.findByFacturePublicId(facture.getPublicId());
        if (existingOpt.isPresent()
                && Boolean.FALSE.equals(existingOpt.get().getStatusDel())
                && existingOpt.get().getStatutPaiement() == StatutPaiementEnum.EN_ATTENTE
                && PROVIDER_FEDAPAY.equals(existingOpt.get().getProvider())
                && StringUtils.hasText(existingOpt.get().getIdTransaction())) {
            DetailsPaiement pending = existingOpt.get();
            // Ne pas renvoyer earlyResponse : laisser checkout() vérifier le statut FedaPay.
            return new CheckoutPrep(
                    null,
                    demande.getCodeSuivie(),
                    facture.getPublicId(),
                    pending.getId(),
                    mode,
                    fees,
                    null,
                    null,
                    null,
                    demande.getTelFidele(),
                    pending.getIdTransaction(),
                    pending.getPaymentUrl()
            );
        }

        Map<String, Object> customer = buildCustomer(demande);
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("codeSuivie", demande.getCodeSuivie());
        metadata.put("facturePublicId", facture.getPublicId().toString());
        metadata.put("refFacture", facture.getRefFacture());

        return new CheckoutPrep(
                null,
                demande.getCodeSuivie(),
                facture.getPublicId(),
                existingOpt.map(DetailsPaiement::getId).orElse(null),
                mode,
                fees,
                customer,
                metadata,
                buildCallbackUrl(demande.getCodeSuivie()),
                demande.getTelFidele(),
                null,
                null
        );
    }

    private PaymentCheckoutResponse persistCheckout(
            CheckoutPrep prep,
            FedaPayClient.CreatedTransaction created,
            FedaPayClient.PaymentToken token
    ) {
        Demande demande = requireDemande(prep.codeSuivie());
        Facture facture = requireFacture(demande);

        DetailsPaiement details = prep.existingDetailsId() != null
                ? detailsPaiementRepository.findById(prep.existingDetailsId()).orElseGet(DetailsPaiement::new)
                : detailsPaiementRepository.findByFacturePublicId(facture.getPublicId()).orElseGet(DetailsPaiement::new);

        details.setStatusDel(false);
        details.setDateDetailsPaiement(LocalDateTime.now());
        details.setMontant(prep.fees().montantFacture());
        applyFeeFields(details, prep.fees());
        details.setProvider(PROVIDER_FEDAPAY);
        details.setPaymentUrl(token.url());
        details.setIdTransaction(String.valueOf(created.id()));
        details.setStatutPaiement(StatutPaiementEnum.EN_ATTENTE);
        details.setNumero(prep.telFidele());
        details.setTypePaiement(demande.getTypePaiement());
        details.setFacture(facture);
        detailsPaiementRepository.save(details);

        syncPaymentStatus(facture, StatutPaiementEnum.EN_ATTENTE, null);

        return checkoutResponse(
                demande, prep.mode(), prep.fees(), true,
                token.url(),
                String.valueOf(created.id()),
                "Redirection vers FedaPay."
        );
    }

    /** Régénère / rafraîchit l'URL de paiement pour une transaction FedaPay déjà créée. */
    private PaymentCheckoutResponse persistReusedToken(
            CheckoutPrep prep,
            long transactionId,
            FedaPayClient.PaymentToken token
    ) {
        Demande demande = requireDemande(prep.codeSuivie());
        Facture facture = requireFacture(demande);
        DetailsPaiement details = prep.existingDetailsId() != null
                ? detailsPaiementRepository.findById(prep.existingDetailsId()).orElse(null)
                : detailsPaiementRepository.findByFacturePublicId(facture.getPublicId()).orElse(null);
        if (details == null) {
            throw new BusinessRuleException("Détails de paiement introuvables pour réutilisation");
        }
        details.setPaymentUrl(token.url());
        details.setIdTransaction(String.valueOf(transactionId));
        details.setStatutPaiement(StatutPaiementEnum.EN_ATTENTE);
        details.setStatusDel(false);
        detailsPaiementRepository.save(details);
        return checkoutResponse(
                demande, prep.mode(), prep.fees(), true,
                token.url(),
                String.valueOf(transactionId),
                "Redirection vers FedaPay."
        );
    }

    private record CheckoutPrep(
            PaymentCheckoutResponse earlyResponse,
            String codeSuivie,
            UUID facturePublicId,
            Long existingDetailsId,
            ModePaiement mode,
            PaymentFeeBreakdown fees,
            Map<String, Object> customer,
            Map<String, String> metadata,
            String callbackUrl,
            String telFidele,
            String reuseTransactionId,
            String reusePaymentUrl
    ) {
        static CheckoutPrep done(PaymentCheckoutResponse response) {
            return new CheckoutPrep(
                    response, null, null, null, null, null, null, null, null, null, null, null
            );
        }
    }

    private PaymentCheckoutResponse quoteCheckoutSnapshot(String codeSuivie, String message) {
        Demande demande = requireDemande(codeSuivie);
        Facture facture = requireFacture(demande);
        ModePaiement mode = resolveMode(demande);
        PaymentFeeBreakdown fees = feeCalculator.calculate(facture.getMontant(), mode);
        DetailsPaiement details = detailsPaiementRepository
                .findByFacturePublicIdAndStatusDelFalse(facture.getPublicId())
                .orElse(null);
        boolean online = details != null
                && PROVIDER_FEDAPAY.equals(details.getProvider())
                && StringUtils.hasText(details.getPaymentUrl())
                && demande.getStatutPaiement() != StatutPaiementEnum.PAYE;
        return checkoutResponse(
                demande,
                mode,
                fees,
                online,
                details != null ? details.getPaymentUrl() : null,
                details != null ? details.getIdTransaction() : null,
                message
        );
    }

    private String fetchRemoteStatus(String transactionId) {
        try {
            long id = Long.parseLong(transactionId.trim());
            JsonNode tx = fedaPayClient.getTransaction(id);
            return text(tx, "status");
        } catch (Exception ex) {
            log.warn("Impossible de lire le statut FedaPay {}: {}", transactionId, ex.getMessage());
            return null;
        }
    }

    private static boolean isApprovedStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String s = status.trim().toLowerCase();
        return "approved".equals(s) || "transferred".equals(s);
    }

    private static boolean isFailedStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String s = status.trim().toLowerCase();
        return "declined".equals(s) || "canceled".equals(s) || "cancelled".equals(s)
                || "failed".equals(s) || "expired".equals(s);
    }

    /**
     * Vérifie la signature immédiatement (thread HTTP) avant tout traitement async.
     */
    public void verifyWebhookSignature(String payload, String signatureHeader) {
        webhookVerifier.verify(payload, signatureHeader);
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        webhookVerifier.verify(payload, signatureHeader);
        try {
            JsonNode event = objectMapper.readTree(payload);
            String name = text(event, "name");
            if (!StringUtils.hasText(name)) {
                name = text(event, "type");
            }
            JsonNode entity = event.path("entity");
            if (entity.isMissingNode() || entity.isNull()) {
                entity = event.path("object");
            }
            if (entity.isMissingNode() || entity.isNull()) {
                entity = event.path("data");
            }

            long transactionId = extractTransactionId(entity);
            if (transactionId <= 0) {
                log.warn("Webhook FedaPay sans id transaction: {}", name);
                return;
            }

            String entityStatus = text(entity, "status");
            String txKey = String.valueOf(transactionId);

            if ("transaction.approved".equals(name) || isApprovedStatus(entityStatus)) {
                if (!markPaid(txKey)) {
                    subscriptionBillingService.activateFromProviderTransaction(txKey);
                }
            } else if ("transaction.declined".equals(name)
                    || "transaction.canceled".equals(name)
                    || "transaction.cancelled".equals(name)
                    || isFailedStatus(entityStatus)) {
                if (!markFailed(txKey)) {
                    subscriptionBillingService.markFailedFromProviderTransaction(txKey);
                }
            } else {
                log.debug("Webhook FedaPay ignoré: {} status={}", name, entityStatus);
            }
        } catch (BusinessRuleException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Échec traitement webhook FedaPay", ex);
            throw new BusinessRuleException("Payload webhook FedaPay invalide");
        }
    }

    /**
     * Applique le barème de frais pour un paiement manuel (secrétariat).
     */
    public void applyFeeSnapshot(DetailsPaiement details, ModePaiement mode, int montantFacture) {
        PaymentFeeBreakdown fees = feeCalculator.calculate(montantFacture, mode);
        details.setMontant(fees.montantFacture());
        applyFeeFields(details, fees);
        if (!StringUtils.hasText(details.getProvider())) {
            details.setProvider(PROVIDER_MANUAL);
        }
    }

    private boolean markPaid(String transactionId) {
        DetailsPaiement details = detailsPaiementRepository
                .findByIdTransactionAndStatusDelFalse(transactionId)
                .orElse(null);
        if (details == null) {
            return false;
        }
        if (details.getStatutPaiement() == StatutPaiementEnum.PAYE) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        details.setStatutPaiement(StatutPaiementEnum.PAYE);
        details.setDateDetailsPaiement(now);
        detailsPaiementRepository.save(details);
        syncPaymentStatus(details.getFacture(), StatutPaiementEnum.PAYE, now);

        Facture facture = details.getFacture();
        Demande demande = facture != null ? facture.getDemande() : null;
        if (demande != null && demande.getParoisse() != null) {
            int credit = details.getMontantNet() != null ? details.getMontantNet() : details.getMontant();
            parishLedgerService.creditMesse(
                    demande.getParoisse(),
                    credit,
                    demande.getCodeSuivie(),
                    "FEDAPAY:" + transactionId
            );
        }
        log.info("Paiement FedaPay approuvé: {}", transactionId);
        return true;
    }

    private boolean markFailed(String transactionId) {
        DetailsPaiement details = detailsPaiementRepository
                .findByIdTransactionAndStatusDelFalse(transactionId)
                .orElse(null);
        if (details == null) {
            return false;
        }
        if (details.getStatutPaiement() == StatutPaiementEnum.PAYE) {
            return true;
        }
        details.setStatutPaiement(StatutPaiementEnum.ECHOUE);
        detailsPaiementRepository.save(details);
        syncPaymentStatus(details.getFacture(), StatutPaiementEnum.ECHOUE, null);
        log.info("Paiement FedaPay échoué/annulé: {}", transactionId);
        return true;
    }

    private void applyFeeFields(DetailsPaiement details, PaymentFeeBreakdown fees) {
        details.setMontantFrais(fees.montantFrais());
        details.setMontantFraisAgregeateur(fees.montantFraisAgregeateur());
        details.setMontantFraisPlateforme(fees.montantFraisPlateforme());
        details.setMontantCharge(fees.montantCharge());
        details.setMontantNet(fees.montantNetParoisse());
    }

    private void syncPaymentStatus(Facture facture, StatutPaiementEnum statut, LocalDateTime datePaiement) {
        facture.setStatutPaiement(statut);
        facture.setDatePaiement(datePaiement);
        factureRepository.save(facture);
        Demande demande = facture.getDemande();
        if (demande != null) {
            demande.setStatutPaiement(statut);
            demandeRepository.save(demande);
        }
    }

    private Demande requireDemande(String codeSuivie) {
        String code = BusinessCodeGenerator.normalizeDemandeTrackingCode(codeSuivie);
        return demandeRepository.findByCodeSuivieWithAssociations(code)
                .or(() -> demandeRepository.findByCodeSuivieWithAssociations(
                        codeSuivie == null ? "" : codeSuivie.trim()))
                .orElseThrow(() -> new TrackingIdNotFoundException("Code de suivi introuvable"));
    }

    private Facture requireFacture(Demande demande) {
        return factureRepository.findByDemandePublicIdAndStatusDelFalse(demande.getPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable pour cette demande"));
    }

    private ModePaiement resolveMode(Demande demande) {
        if (demande.getTypePaiement() == null || demande.getTypePaiement().getMode() == null) {
            throw new BusinessRuleException("Type de paiement manquant sur la demande");
        }
        return demande.getTypePaiement().getMode();
    }

    private Map<String, Object> buildCustomer(Demande demande) {
        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("firstname", blankToDash(demande.getPrenomFidele()));
        customer.put("lastname", blankToDash(demande.getNomFidele()));
        // FedaPay recommande email + téléphone ; email technique si absent.
        if (StringUtils.hasText(demande.getEmailFidele())) {
            customer.put("email", demande.getEmailFidele().trim());
        } else if (StringUtils.hasText(demande.getCodeSuivie())) {
            String safe = demande.getCodeSuivie().replaceAll("[^A-Za-z0-9]", "").toLowerCase();
            customer.put("email", "fidele+" + safe + "@pay.missanye.local");
        }
        Map<String, Object> phone = FedaPayPhone.toCustomerPhone(
                demande.getTelFidele(),
                properties.getCustomerCountry()
        );
        if (phone != null) {
            customer.put("phone_number", phone);
        } else if (StringUtils.hasText(demande.getTelFidele())) {
            log.warn(
                    "Téléphone fidèle non envoyé à FedaPay (format invalide): code={}",
                    demande.getCodeSuivie()
            );
        }
        return customer;
    }

    private String buildCallbackUrl(String codeSuivie) {
        String base = properties.getCallbackBaseUrl();
        if (!StringUtils.hasText(base)) {
            return null;
        }
        String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        // URL opaque : /paiement/retour?r=<jeton signé> — pas le code de suivi en clair.
        String token = paymentReturnTokenService.issue(codeSuivie);
        return trimmed + "/retour?r=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
    }

    private PaymentCheckoutResponse checkoutResponse(
            Demande demande,
            ModePaiement mode,
            PaymentFeeBreakdown fees,
            boolean online,
            String paymentUrl,
            String providerTxId,
            String message
    ) {
        return new PaymentCheckoutResponse(
                demande.getCodeSuivie(),
                demande.getStatutPaiement(),
                mode,
                online,
                paymentUrl,
                providerTxId,
                fees.montantFacture(),
                fees.montantFraisAgregeateur(),
                fees.montantFraisPlateforme(),
                fees.montantFrais(),
                fees.montantCharge(),
                fees.montantNetParoisse(),
                fees.montantNetPlateforme(),
                fees.feePercentAgregeateur(),
                fees.feePercentPlateforme(),
                fees.feePayer().name(),
                message
        );
    }

    private static long extractTransactionId(JsonNode entity) {
        if (entity == null || entity.isMissingNode() || entity.isNull()) {
            return -1;
        }
        if (entity.has("id") && entity.get("id").canConvertToLong()) {
            return entity.get("id").asLong();
        }
        if (entity.has("transaction_id") && entity.get("transaction_id").canConvertToLong()) {
            return entity.get("transaction_id").asLong();
        }
        JsonNode nested = entity.path("transaction");
        if (nested.has("id") && nested.get("id").canConvertToLong()) {
            return nested.get("id").asLong();
        }
        return -1;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String blankToDash(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }
}
