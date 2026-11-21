package com.eyram.dev.church_project_spring.service.billing;

import com.eyram.dev.church_project_spring.DTO.response.AbonnementResponse;
import com.eyram.dev.church_project_spring.config.CacheConfig;
import com.eyram.dev.church_project_spring.config.PlatformBillingProperties;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAbonnement;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.PlanSaas;
import com.eyram.dev.church_project_spring.enums.StatutAbonnement;
import com.eyram.dev.church_project_spring.enums.StatutTenant;
import com.eyram.dev.church_project_spring.repositories.ParoisseAbonnementRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.PlanSaasService;
import com.eyram.dev.church_project_spring.service.audit.AdministrativeAuditService;
import com.eyram.dev.church_project_spring.service.ProfessionalEmailService;
import com.eyram.dev.church_project_spring.service.tenant.TenantCatalogBootstrapService;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionBillingService {

    private static final DateTimeFormatter ECHEANCE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ParoisseRepository paroisseRepository;
    private final ParoisseAbonnementRepository abonnementRepository;
    private final com.eyram.dev.church_project_spring.service.payment.saspay.SasPayClient sasPayClient;
    private final com.eyram.dev.church_project_spring.service.payment.saspay.SasPayCheckoutStore sasPayStore;
    private final com.eyram.dev.church_project_spring.config.SasPayProperties sasPayProperties;
    private final PlatformBillingProperties properties;
    private final PlanSaasService planSaasService;
    private final TenantCatalogBootstrapService tenantCatalogBootstrapService;
    private final TenantAccessService tenantAccessService;
    private final ProfessionalEmailService professionalEmailService;
    private final ParoisseAccessRepository paroisseAccessRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;
    private final CacheManager cacheManager;
    private final AdministrativeAuditService administrativeAuditService;

    @Transactional
    public ParoisseAbonnement createPending(Paroisse paroisse, String plan) {
        PlanSaas pricing = planSaasService.requireActive(plan);

        ParoisseAbonnement abonnement = new ParoisseAbonnement();
        abonnement.setParoisse(paroisse);
        abonnement.setPlan(pricing.getCode());
        abonnement.setMontant(pricing.getMontantXof());
        abonnement.setDureeMois(pricing.getDureeMois());
        abonnement.setStatut(StatutAbonnement.EN_ATTENTE);
        abonnement.setStatusDel(false);
        return abonnementRepository.save(abonnement);
    }

    /** New subscriptions use SasPay; existing provider references are never replaced. */
    public Map<String, Object> checkout(UUID paroissePublicId, String plan) {
        return transactionTemplate.execute(
                status -> {
                    Paroisse parish =
                            paroisseRepository
                                    .findByPublicIdForUpdate(paroissePublicId)
                                    .orElseThrow(
                                            () ->
                                                    new ResourceNotFoundException(
                                                            "Paroisse introuvable"));
                    tenantAccessService.checkParoisseAccess(parish);
                    String effective =
                            StringUtils.hasText(plan)
                                    ? planSaasService.requireActive(plan).getCode()
                                    : planSaasService.requireDefaultActive().getCode();
                    ParoisseAbonnement subscription =
                            abonnementRepository
                                    .findByParoisseAndStatusDelFalseOrderByCreatedAtDesc(parish)
                                    .stream()
                                    .filter(
                                            a ->
                                                    a.getStatut() == StatutAbonnement.EN_ATTENTE
                                                            && effective.equals(a.getPlan()))
                                    .findFirst()
                                    .orElseGet(() -> createPending(parish, effective));
                    var result = new LinkedHashMap<String, Object>();
                    result.put("abonnementPublicId", subscription.getPublicId());
                    result.put("plan", effective);
                    result.put("montant", subscription.getMontant());
                    result.put(
                            "paymentUrl",
                            subscription.getPaymentUrl() == null
                                    ? ""
                                    : subscription.getPaymentUrl());
                    result.put(
                            "providerTransactionId",
                            subscription.getIdTransaction() == null
                                    ? ""
                                    : subscription.getIdTransaction());
                    if (subscription.getIdTransaction() != null
                            && !subscription.getIdTransaction().startsWith("SASPAY:")) {
                        result.put(
                                "message",
                                "Une ancienne session de paiement reste ouverte. Vérifiez-la avant"
                                    + " un nouveau règlement.");
                        return result;
                    }
                    if (!sasPayClient.configured()) {
                        result.put(
                                "message",
                                "SasPay désactivé — configurez le marchand pour encaisser en"
                                    + " ligne.");
                        return result;
                    }
                    if (!StringUtils.hasText(parish.getEmail()))
                        throw new com.eyram.dev.church_project_spring.utils.exception
                                .BusinessRuleException(
                                "Une adresse e-mail paroissiale est nécessaire pour le paiement"
                                    + " SasPay.");
                    var body = new LinkedHashMap<String, Object>();
                    body.put(
                            "amount",
                            java.math.BigDecimal.valueOf(subscription.getMontant())
                                    .setScale(2)
                                    .toPlainString());
                    body.put("currency", "XOF");
                    body.put("country", sasPayProperties.getCountry());
                    body.put("description", "Abonnement Missanye — " + effective);
                    body.put("customer_email", parish.getEmail());
                    body.put("customer_name", parish.getNom());
                    body.put(
                            "return_url",
                            sasPayProperties.getAppUrl().replaceAll("/+$", "") + "/admin");
                    body.put("fee_charge_mode", "ADD_ON");
                    body.put("expires_at", java.time.Instant.now().plusSeconds(3300).toString());
                    var checkout =
                            sasPayStore.start(
                                    "ABONNEMENT",
                                    subscription.getPublicId(),
                                    subscription.getMontant(),
                                    0,
                                    body);
                    if ("PENDING".equals(checkout.state())) {
                        subscription.setIdTransaction(checkout.reference());
                        subscription.setPaymentUrl(checkout.url());
                        abonnementRepository.save(subscription);
                        result.put("paymentUrl", checkout.url());
                        result.put("providerTransactionId", checkout.reference());
                    } else
                        result.put(
                                "message",
                                checkout.error() == null
                                        ? "La session nécessite une vérification avant un nouvel"
                                              + " essai."
                                        : checkout.error());
                    return result;
                });
    }

    @Transactional
    public void activateFromProviderTransaction(String transactionId) {
        ParoisseAbonnement candidate = abonnementRepository
                .findByIdTransactionAndStatusDelFalse(transactionId)
                .orElse(null);
        if (candidate == null) {
            log.warn("Abonnement introuvable pour transaction {}", transactionId);
            return;
        }

        // Ordre de verrouillage unique : paroisse, puis abonnement. Deux
        // paiements distincts reçus simultanément pour la même paroisse ne
        // peuvent ainsi ni perdre ni doubler une prolongation.
        paroisseRepository.findByPublicIdForUpdate(candidate.getParoisse().getPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        ParoisseAbonnement abonnement = abonnementRepository
                .findByIdTransactionForUpdate(transactionId)
                .orElse(null);
        if (abonnement == null) {
            log.warn(
                    "Abonnement supprimé pendant le traitement de la transaction {}",
                    transactionId);
            return;
        }

        // Une transaction ne paie qu'une période : un webhook rejoué ne doit
        // pas prolonger l'échéance une seconde fois.
        if (abonnement.getStatut() == StatutAbonnement.ACTIF) {
            log.info("Transaction {} déjà encaissée, activation ignorée", transactionId);
            return;
        }
        activateAbonnement(
                abonnement,
                transactionId.startsWith("SASPAY:") ? "SASPAY" : "FEDAPAY",
                transactionId.startsWith("SASPAY:") ? "SasPay" : "Agrégateur FedaPay");
    }

    /**
     * Active l'accès paroisse (paiement confirmé hors ligne ou grace période).
     * Après cet appel, l'admin local peut se connecter.
     */
    @Transactional
    public Map<String, Object> activateManually(UUID paroissePublicId, String plan) {
        Paroisse paroisse = requireClientParoisseForUpdate(paroissePublicId);

        // Un abonnement en attente est honoré ; sinon on ouvre une nouvelle
        // période, ce qui rend le renouvellement possible sur une paroisse déjà active.
        ParoisseAbonnement abonnement = abonnementRepository
                .findByParoisseAndStatusDelFalseOrderByCreatedAtDesc(paroisse)
                .stream()
                .filter(a -> a.getStatut() == StatutAbonnement.EN_ATTENTE)
                .findFirst()
                .orElse(null);

        if (abonnement == null) {
            abonnement = createPending(paroisse, resolvePlan(paroisse, plan));
        } else if (StringUtils.hasText(plan)) {
            PlanSaas pricing = planSaasService.requireActive(plan);
            if (!pricing.getCode().equals(abonnement.getPlan())) {
                abonnement.setPlan(pricing.getCode());
                abonnement.setMontant(pricing.getMontantXof());
                abonnement.setDureeMois(pricing.getDureeMois());
            }
        }

        boolean renouvellement = Boolean.TRUE.equals(paroisse.getIsActive())
                && paroisse.getSubscriptionExpiresAt() != null;

        String by = "Comptable";
        try {
            var user = tenantAccessService.getCurrentUser();
            by = user.getFullName() != null && !user.getFullName().isBlank()
                    ? user.getFullName()
                    : user.getUsername();
        } catch (Exception ignored) {
            // Activation hors contexte utilisateur (job) : libellé générique.
        }
        activateAbonnement(abonnement, "MANUEL", by);
        administrativeAuditService.record(
                AdministrativeAuditService.SUBSCRIPTION_MANUALLY_ACTIVATED,
                "SUBSCRIPTION",
                abonnement.getPublicId(),
                paroisse.getPublicId(),
                "Plan=" + abonnement.getPlan() + "; montantXof=" + abonnement.getMontant()
        );
        return Map.of(
                "paroissePublicId", paroisse.getPublicId(),
                "statut", "ACTIF",
                "plan", abonnement.getPlan(),
                "finAt", abonnement.getFinAt() != null ? abonnement.getFinAt().toString() : "",
                "message", renouvellement
                        ? "Abonnement renouvelé — nouvelle échéance le "
                                + abonnement.getFinAt().format(ECHEANCE_FORMAT)
                        : "Paroisse activée — l'administrateur peut se connecter"
        );
    }

    /** Reconduit le plan en cours à défaut d'indication explicite. */
    private String resolvePlan(Paroisse paroisse, String plan) {
        if (StringUtils.hasText(plan)) {
            return planSaasService.requireActive(plan).getCode();
        }

        String currentPlan = abonnementRepository
                .findFirstByParoisseAndStatutAndStatusDelFalseOrderByFinAtDesc(
                        paroisse, StatutAbonnement.ACTIF
                )
                .map(ParoisseAbonnement::getPlan)
                .orElse(null);

        if (StringUtils.hasText(currentPlan)) {
            try {
                return planSaasService.requireActive(currentPlan).getCode();
            } catch (RuntimeException ignored) {
                // Le plan historique a pu être désactivé : on bascule alors
                // vers la formule active recommandée plutôt que de bloquer le renouvellement.
            }
        }
        return planSaasService.requireDefaultActive().getCode();
    }

    /**
     * Ouvre ou prolonge une période d'abonnement.
     * <p>
     * L'échéance court à partir de l'échéance en cours lorsqu'elle n'est pas
     * dépassée : une paroisse qui renouvelle en avance conserve ses jours
     * restants au lieu de les perdre.
     */
    private void activateAbonnement(ParoisseAbonnement abonnement, String source, String activatedByNom) {
        Paroisse paroisse = abonnement.getParoisse();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime enCours = paroisse != null ? paroisse.getSubscriptionExpiresAt() : null;
        LocalDateTime depart = (enCours != null && enCours.isAfter(now)) ? enCours : now;
        LocalDateTime fin = depart.plusMonths(abonnement.getDureeMois());

        abonnement.setStatut(StatutAbonnement.ACTIF);
        abonnement.setDebutAt(abonnement.getDebutAt() != null ? abonnement.getDebutAt() : now);
        abonnement.setFinAt(fin);
        abonnement.setActivatedAt(now);
        abonnement.setActivationSource(source);
        abonnement.setActivatedByNom(activatedByNom);
        abonnementRepository.save(abonnement);

        if (paroisse != null) {
            paroisse.appliquerStatut(StatutTenant.ACTIVE);
            paroisse.setSubscriptionExpiresAt(fin);
            if (!professionalEmailService.isProfessional(paroisse.getEmail())) {
                professionalEmailService.assignToParoisse(paroisse);
            }
            paroisseRepository.save(paroisse);
            // Première activation : la paroisse reçoit le catalogue plateforme
            // (horaires, types, forfaits) si elle n'en a pas encore.
            tenantCatalogBootstrapService.seedDefaultsIfEmpty(paroisse);
            for (ParoisseAccess access : paroisseAccessRepository.findByParoisseAndStatusDelFalse(paroisse)) {
                if (access.getUser() != null && !professionalEmailService.isProfessional(access.getUser().getEmail())) {
                    professionalEmailService.assignToUser(access.getUser(), paroisse);
                    userRepository.save(access.getUser());
                }
            }
            log.info(
                    "Abonnement actif pour paroisse {} jusqu'au {} ({})",
                    paroisse.getNom(),
                    fin,
                    source);
            evictPublicHorairesCache();
        }
    }

    private void evictPublicHorairesCache() {
        Cache cache = cacheManager.getCache(CacheConfig.HORAIRES_PUBLIC_ACTIVES);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Balaie les échéances : l'abonnement passe en {@code EXPIRE} dès le terme,
     * puis l'accès de la paroisse est coupé une fois la tolérance écoulée.
     * Sans ce balayage, un abonnement échu resterait indéfiniment « ACTIF ».
     *
     * @return nombre d'abonnements et de paroisses dont l'état a changé
     */
    @Transactional
    public int sweepEcheances() {
        LocalDateTime now = LocalDateTime.now();
        int changed = 0;

        for (ParoisseAbonnement abonnement : abonnementRepository
                .findByStatutAndStatusDelFalseAndFinAtBefore(StatutAbonnement.ACTIF, now)) {
            abonnement.setStatut(StatutAbonnement.EXPIRE);
            abonnementRepository.save(abonnement);
            changed++;

            Paroisse paroisse = abonnement.getParoisse();
            if (paroisse != null && paroisse.getStatutTenant() == StatutTenant.ACTIVE) {
                paroisse.appliquerStatut(StatutTenant.EN_TOLERANCE);
                paroisseRepository.save(paroisse);
            }

            log.info(
                    "Abonnement échu le {} pour la paroisse {}",
                    abonnement.getFinAt(),
                    paroisse != null ? paroisse.getNom() : "?"
            );
        }

        LocalDateTime finTolerance = now.minusDays(Math.max(0, properties.getSubscriptionGraceDays()));
        for (Paroisse paroisse : paroisseRepository
                .findByIsActiveTrueAndStatusDelFalseAndSubscriptionExpiresAtBefore(finTolerance)) {
            paroisse.appliquerStatut(StatutTenant.SUSPENDUE);
            paroisseRepository.save(paroisse);
            changed++;
            log.warn(
                    "Accès suspendu pour {} : échéance du {} dépassée au-delà de la tolérance de {}"
                        + " jours",
                    paroisse.getNom(),
                    paroisse.getSubscriptionExpiresAt(),
                    properties.getSubscriptionGraceDays());
        }

        if (changed > 0) {
            evictPublicHorairesCache();
        }
        return changed;
    }

    @Transactional(readOnly = true)
    public List<AbonnementResponse> listAbonnements() {
        LocalDateTime now = LocalDateTime.now();
        // Une ligne par paroisse : la période la plus récente (createdAt DESC).
        Map<UUID, AbonnementResponse> latestByParoisse = new LinkedHashMap<>();
        for (ParoisseAbonnement abonnement : abonnementRepository.findAllActiveWithParoisse()) {
            Paroisse paroisse = abonnement.getParoisse();
            if (paroisse == null || Boolean.TRUE.equals(paroisse.getIsSystem())) {
                continue;
            }
            UUID key = paroisse.getPublicId();
            if (!latestByParoisse.containsKey(key)) {
                latestByParoisse.put(key, toResponse(abonnement, now));
            }
        }
        return List.copyOf(latestByParoisse.values());
    }

    /**
     * Prolongation gracieuse (jours) sans nouveau paiement — outil comptable.
     */
    @Transactional
    public Map<String, Object> prolonger(UUID paroissePublicId, int jours) {
        if (jours < 1 || jours > 366) {
            throw new IllegalArgumentException("La prolongation doit être entre 1 et 366 jours");
        }
        Paroisse paroisse = requireClientParoisseForUpdate(paroissePublicId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime enCours = paroisse.getSubscriptionExpiresAt();
        LocalDateTime depart = (enCours != null && enCours.isAfter(now)) ? enCours : now;
        LocalDateTime nouvelleFin = depart.plusDays(jours);

        ParoisseAbonnement abonnement = abonnementRepository
                .findByParoisseAndStatusDelFalseOrderByCreatedAtDesc(paroisse)
                .stream()
                .findFirst()
                .orElseGet(() -> createPending(paroisse, planSaasService.requireDefaultActive().getCode()));

        abonnement.setStatut(StatutAbonnement.ACTIF);
        if (abonnement.getDebutAt() == null) {
            abonnement.setDebutAt(now);
        }
        abonnement.setFinAt(nouvelleFin);
        abonnement.setActivatedAt(now);
        abonnement.setActivationSource("PROLONGATION");
        abonnement.setActivatedByNom(currentActorLabel());
        abonnementRepository.save(abonnement);

        paroisse.appliquerStatut(StatutTenant.ACTIVE);
        paroisse.setSubscriptionExpiresAt(nouvelleFin);
        paroisseRepository.save(paroisse);
        evictPublicHorairesCache();
        administrativeAuditService.record(
                AdministrativeAuditService.SUBSCRIPTION_EXTENDED,
                "SUBSCRIPTION",
                abonnement.getPublicId(),
                paroisse.getPublicId(),
                "Prolongation gracieuse=" + jours + " jour(s); nouvelleEcheance=" + nouvelleFin
        );

        return Map.of(
                "paroissePublicId", paroisse.getPublicId(),
                "statut", "ACTIF",
                "finAt", nouvelleFin.toString(),
                "joursAjoutes", jours,
                "message", "Abonnement prolongé de " + jours + " jour(s) — échéance le "
                        + nouvelleFin.format(ECHEANCE_FORMAT)
        );
    }

    /**
     * Annule un abonnement encore en attente de paiement (lien de paiement abandonné).
     */
    @Transactional
    public Map<String, Object> annulerPending(UUID abonnementPublicId) {
        ParoisseAbonnement abonnement = abonnementRepository.findByPublicIdForUpdate(abonnementPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable"));
        if (abonnement.getStatut() != StatutAbonnement.EN_ATTENTE) {
            throw new IllegalArgumentException(
                    "Seuls les abonnements en attente de paiement peuvent être annulés");
        }
        abonnement.setStatut(StatutAbonnement.ANNULE);
        abonnementRepository.save(abonnement);
        administrativeAuditService.record(
                AdministrativeAuditService.SUBSCRIPTION_PENDING_CANCELLED,
                "SUBSCRIPTION",
                abonnement.getPublicId(),
                abonnement.getParoisse().getPublicId(),
                "Paiement fournisseur abandonné; plan=" + abonnement.getPlan()
        );
        return Map.of(
                "abonnementPublicId", abonnement.getPublicId(),
                "statut", "ANNULE",
                "message", "Paiement d'abonnement annulé"
        );
    }

    /**
     * Résilie l'accès SaaS de la paroisse (sans supprimer son historique).
     */
    @Transactional
    public Map<String, Object> resilier(UUID paroissePublicId) {
        Paroisse paroisse = requireClientParoisseForUpdate(paroissePublicId);
        for (ParoisseAbonnement abonnement : abonnementRepository
                .findByParoisseAndStatusDelFalseOrderByCreatedAtDesc(paroisse)) {
            if (abonnement.getStatut() == StatutAbonnement.ACTIF
                    || abonnement.getStatut() == StatutAbonnement.EN_ATTENTE) {
                abonnement.setStatut(
                        abonnement.getStatut() == StatutAbonnement.EN_ATTENTE
                                ? StatutAbonnement.ANNULE
                                : StatutAbonnement.EXPIRE
                );
                abonnementRepository.save(abonnement);
            }
        }
        paroisse.appliquerStatut(StatutTenant.RESILIEE);
        paroisseRepository.save(paroisse);
        evictPublicHorairesCache();
        administrativeAuditService.record(
                AdministrativeAuditService.SUBSCRIPTION_TERMINATED,
                "PARISH",
                paroisse.getPublicId(),
                paroisse.getPublicId(),
                "Accès SaaS résilié; historique conservé"
        );
        return Map.of(
                "paroissePublicId", paroisse.getPublicId(),
                "statut", "RESILIEE",
                "message", "Paroisse résiliée — accès coupé"
        );
    }

    private Paroisse requireClientParoisseForUpdate(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdForUpdate(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        if (Boolean.TRUE.equals(paroisse.getIsSystem())) {
            throw new IllegalArgumentException(
                    "Le catalogue plateforme n'est pas une paroisse cliente");
        }
        return paroisse;
    }

    private String currentActorLabel() {
        try {
            var user = tenantAccessService.getCurrentUser();
            return user.getFullName() != null && !user.getFullName().isBlank()
                    ? user.getFullName()
                    : user.getUsername();
        } catch (Exception ignored) {
            return "Comptable";
        }
    }

    private AbonnementResponse toResponse(ParoisseAbonnement abonnement, LocalDateTime now) {
        Paroisse paroisse = abonnement.getParoisse();
        LocalDateTime fin = abonnement.getFinAt();
        Long joursRestants = fin != null ? ChronoUnit.DAYS.between(now, fin) : null;
        boolean echue = fin != null && fin.isBefore(now);

        return new AbonnementResponse(
                abonnement.getPublicId(),
                paroisse != null ? paroisse.getPublicId() : null,
                paroisse != null ? paroisse.getNom() : null,
                paroisse != null && paroisse.getDoyenne() != null ? paroisse.getDoyenne().getNom() : null,
                paroisse != null ? paroisse.getEmail() : null,
                paroisse != null ? paroisse.getTelephone() : null,
                paroisse != null && Boolean.TRUE.equals(paroisse.getIsActive()),
                abonnement.getPlan(),
                abonnement.getMontant(),
                abonnement.getStatut(),
                abonnement.getDebutAt(),
                fin,
                abonnement.getActivatedAt(),
                abonnement.getActivationSource(),
                abonnement.getActivatedByNom(),
                joursRestants,
                echue && paroisse != null && Boolean.TRUE.equals(paroisse.getIsActive()),
                !echue && joursRestants != null && joursRestants <= properties.getSubscriptionWarningDays(),
                abonnement.getIdTransaction(),
                abonnement.getPaymentUrl(),
                abonnement.getCreatedAt()
        );
    }

    @Transactional
    public void markFailedFromProviderTransaction(String transactionId) {
        ParoisseAbonnement abonnement = abonnementRepository
                .findByIdTransactionForUpdate(transactionId)
                .orElse(null);
        if (abonnement == null || abonnement.getStatut() == StatutAbonnement.ACTIF) {
            return;
        }
        abonnement.setStatut(StatutAbonnement.ANNULE);
        abonnementRepository.save(abonnement);
    }
}
