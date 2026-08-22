package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.request.CelebrationSlotRequest;
import com.eyram.dev.church_project_spring.DTO.request.DemandeIntentionRequest;
import com.eyram.dev.church_project_spring.DTO.request.DemandeValidationRequest;
import com.eyram.dev.church_project_spring.DTO.response.CelebrationSlotResponse;
import com.eyram.dev.church_project_spring.DTO.response.DemandeParoisseStatsResponse;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.DTO.response.PageResponse;
import com.eyram.dev.church_project_spring.DTO.response.TrackingByPhoneResponse;
import com.eyram.dev.church_project_spring.entities.*;
import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.NatureForfaitEnum;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.enums.StatutValidationEnum;
import com.eyram.dev.church_project_spring.config.CacheConfig;
import com.eyram.dev.church_project_spring.config.DemandePaymentProperties;
import com.eyram.dev.church_project_spring.mappers.DemandeMapper;
import com.eyram.dev.church_project_spring.repositories.*;
import com.eyram.dev.church_project_spring.repositories.projection.DemandeParoisseStatsProjection;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.DemandeService;
import com.eyram.dev.church_project_spring.service.DemandeSchedulingPolicy;
import com.eyram.dev.church_project_spring.service.HoraireService;
import com.eyram.dev.church_project_spring.service.mail.AppMailService;
import com.eyram.dev.church_project_spring.utils.BusinessCodeGenerator;
import com.eyram.dev.church_project_spring.utils.FideleNameUtils;
import com.eyram.dev.church_project_spring.utils.ForfaitDureeLabels;
import com.eyram.dev.church_project_spring.utils.IntentionTextUtils;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import com.eyram.dev.church_project_spring.utils.exception.TrackingIdNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DemandeServiceImpl implements DemandeService {

    /** Identité anonyme lorsqu'un fidèle ne renseigne pas nom / prénom. */
    static final String DEFAULT_FIDELE_NAME = FideleNameUtils.DEFAULT;

    private static final String SYSTEM_UNPAID_CANCEL_ACTOR =
            "Système — impayé avant célébration";

    private static final DateTimeFormatter REMINDER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter REMINDER_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH' h 'mm");

    /** Intervalle minimum entre deux rappels (le cron tourne toutes les 6 h). */
    private static final long REMINDER_MIN_HOURS = 5;

    /** Lettres Unicode, espaces, tirets et apostrophes uniquement. */
    private static final Pattern PERSON_NAME_PATTERN =
            Pattern.compile("^[\\p{L}]+(?:[\\s'\\-]+[\\p{L}]+)*$");

    private final DemandeRepository demandeRepository;
    private final ParoisseRepository paroisseRepository;
    private final TypeDemandeRepository typeDemandeRepository;
    private final ForfaitTarifRepository forfaitTarifRepository;
    private final HoraireRepository horaireRepository;
    private final UserRepository userRepository;
    private final DemandeMapper demandeMapper;
    private final TypePaiementRepository typePaiementRepository;
    private final FactureRepository factureRepository;
    private final DetailsPaiementRepository detailsPaiementRepository;
    private final DemandeDateRepository demandeDateRepository;
    private final TenantAccessService tenantAccessService;
    private final DemandeSchedulingPolicy demandeSchedulingPolicy;
    private final HoraireService horaireService;
    private final DemandePaymentProperties demandePaymentProperties;
    private final Clock clock;
    private final AppMailService appMailService;
    private final CacheManager cacheManager;

    @Override
    @CacheEvict(cacheNames = {
            CacheConfig.DASHBOARD_STATS,
            CacheConfig.DASHBOARD_PROGRAMMES,
            CacheConfig.DEMANDE_PAGES
    }, allEntries = true)
    public DemandeResponse create(DemandeRequest request) {

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        requireActive(Boolean.TRUE.equals(paroisse.getIsActive()), "Cette paroisse n'accepte plus de nouvelles demandes");

        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(request.typeDemandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));
        requireActive(Boolean.TRUE.equals(typeDemande.getIsActive()), "Ce type de demande n'est plus disponible");

        validateTypeDemandeParoisse(typeDemande, paroisse);

        ForfaitTarif forfaitTarif = forfaitTarifRepository.findByPublicIdAndStatusDelFalse(request.forfaitTarifPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Forfait tarif introuvable"));
        requireActive(Boolean.TRUE.equals(forfaitTarif.getIsActive()), "Ce forfait n'est plus disponible");

        if (!forfaitTarif.getTypeDemande().getPublicId().equals(typeDemande.getPublicId())) {
            throw new IllegalArgumentException("Le forfait ne correspond pas au type de demande");
        }

        Horaire horaire = null;
        if (request.horairePublicId() != null) {
            horaire = horaireRepository.findByPublicIdAndStatusDelFalse(request.horairePublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable"));
            requireActive(Boolean.TRUE.equals(horaire.getIsActive()), "Cet horaire n'est plus disponible");

            if (!horaire.getParoisse().getPublicId().equals(paroisse.getPublicId())) {
                throw new IllegalArgumentException("L'horaire ne correspond pas à la paroisse choisie");
            }
        }

        // Tarif selon le jour / nature d'honoraire du créneau (NORMALE vs DOMINICALE…).
        forfaitTarif = resolveForfaitForCelebrationDay(typeDemande, forfaitTarif, request, horaire);

        User user = null;
        if (request.userPublicId() != null) {
            user = userRepository.findByPublicIdAndStatusDelFalse(request.userPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        }

        validateSpecialeContact(request, forfaitTarif);
        validateSpecialeContact(request, forfaitTarif);
        validateDates(request, forfaitTarif);
        if (ForfaitDureeLabels.isMultiCelebration(forfaitTarif.getNombreCelebration())) {
            if (!hasCelebrationSlots(request)) {
                throw new BusinessRuleException(
                        "Chaque date du "
                                + ForfaitDureeLabels.labelFor(forfaitTarif.getNombreCelebration())
                                + " doit avoir un créneau adapté au jour de célébration"
                );
            }
            validateCelebrationSlots(request, paroisse, typeDemande, forfaitTarif);
            // Horaire « principal » = 1er créneau (compat affichages legacy).
            horaire = resolveFirstSlotHoraire(request, paroisse);
        } else {
            validateHoraire(request.heurePersonnalisee(), horaire, forfaitTarif);
            validateCelebrationSchedule(request, horaire, typeDemande, forfaitTarif);
        }

        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(request.typePaiementPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement introuvable"));

        String intention = IntentionTextUtils.requireValid(request.intention());

        Demande demande = demandeMapper.dtoToModel(request);
        demande.setIntention(intention);
        applyFideleIdentity(demande, request);
        demande.setParoisse(paroisse);
        demande.setTypeDemande(typeDemande);
        demande.setForfaitTarif(forfaitTarif);
        demande.setHoraire(horaire);
        demande.setUser(user);
        demande.setTypePaiement(typePaiement);
        demande.setMontant(forfaitTarif.getMontantForfait());
        demande.setCodeSuivie(generateTrackingCode(paroisse));

        applyInitialStatuses(demande, forfaitTarif);

        Demande savedDemande = demandeRepository.save(demande);

        generateDemandeDates(savedDemande, request, typeDemande, forfaitTarif);

        Facture facture = new Facture();
        facture.setDemande(savedDemande);
        facture.setMontant(toFactureMontant(savedDemande.getMontant()));
        facture.setStatutPaiement(savedDemande.getStatutPaiement());
        facture.setDatePaiement(null);
        facture.setRefFacture(generateFactureReference(paroisse));

        factureRepository.save(facture);

        return buildDemandeResponse(savedDemande);
    }

    @Override
    @CacheEvict(cacheNames = {
            CacheConfig.DASHBOARD_STATS,
            CacheConfig.DASHBOARD_PROGRAMMES,
            CacheConfig.DEMANDE_PAGES
    }, allEntries = true)
    public DemandeResponse update(UUID publicId, DemandeRequest request) {

        Demande existingDemande = demandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        tenantAccessService.checkParoisseAccess(existingDemande.getParoisse());

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        requireActive(Boolean.TRUE.equals(paroisse.getIsActive()),
                "Cette paroisse n'accepte plus de modifications de demandes");

        tenantAccessService.checkParoisseAccess(paroisse);

        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(request.typeDemandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));
        requireActive(Boolean.TRUE.equals(typeDemande.getIsActive()),
                "Ce type de demande n'est plus disponible");

        validateTypeDemandeParoisse(typeDemande, paroisse);

        ForfaitTarif forfaitTarif = forfaitTarifRepository.findByPublicIdAndStatusDelFalse(request.forfaitTarifPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Forfait tarif introuvable"));
        requireActive(Boolean.TRUE.equals(forfaitTarif.getIsActive()),
                "Ce forfait n'est plus disponible");

        if (!forfaitTarif.getTypeDemande().getPublicId().equals(typeDemande.getPublicId())) {
            throw new IllegalArgumentException("Le forfait ne correspond pas au type de demande");
        }

        Horaire horaire = null;
        if (request.horairePublicId() != null) {
            horaire = horaireRepository.findByPublicIdAndStatusDelFalse(request.horairePublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable"));
            requireActive(Boolean.TRUE.equals(horaire.getIsActive()),
                    "Cet horaire n'est plus disponible");

            if (!horaire.getParoisse().getPublicId().equals(paroisse.getPublicId())) {
                throw new IllegalArgumentException("L'horaire ne correspond pas à la paroisse choisie");
            }
        }

        forfaitTarif = resolveForfaitForCelebrationDay(typeDemande, forfaitTarif, request, horaire);

        User user = null;
        if (request.userPublicId() != null) {
            user = userRepository.findByPublicIdAndStatusDelFalse(request.userPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        }

        validateDates(request, forfaitTarif);
        if (ForfaitDureeLabels.isMultiCelebration(forfaitTarif.getNombreCelebration())) {
            if (!hasCelebrationSlots(request)) {
                throw new BusinessRuleException(
                        "Chaque date du "
                                + ForfaitDureeLabels.labelFor(forfaitTarif.getNombreCelebration())
                                + " doit avoir un créneau adapté au jour de célébration"
                );
            }
            validateCelebrationSlots(request, paroisse, typeDemande, forfaitTarif);
            horaire = resolveFirstSlotHoraire(request, paroisse);
        } else {
            validateHoraire(request.heurePersonnalisee(), horaire, forfaitTarif);
            validateCelebrationSchedule(request, horaire, typeDemande, forfaitTarif);
        }

        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(request.typePaiementPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement introuvable"));

        NatureForfaitEnum previousNature = existingDemande.getForfaitTarif() != null
                ? existingDemande.getForfaitTarif().getNatureForfait()
                : null;

        String intention = IntentionTextUtils.requireValid(request.intention());

        demandeMapper.updateEntityFromDto(request, existingDemande);
        existingDemande.setIntention(intention);
        applyFideleIdentity(existingDemande, request);
        existingDemande.setParoisse(paroisse);
        existingDemande.setTypeDemande(typeDemande);
        existingDemande.setForfaitTarif(forfaitTarif);
        existingDemande.setHoraire(horaire);
        existingDemande.setUser(user);
        existingDemande.setTypePaiement(typePaiement);
        existingDemande.setMontant(forfaitTarif.getMontantForfait());

        // Ne jamais réinitialiser le statut de paiement à la mise à jour.
        // Recalculer uniquement la validation si la nature du forfait change.
        if (previousNature != forfaitTarif.getNatureForfait()) {
            applyValidationStatuses(existingDemande, forfaitTarif);
        }

        Demande updatedDemande = demandeRepository.save(existingDemande);

        refreshDemandeDates(updatedDemande, request, typeDemande, forfaitTarif);

        Facture facture = factureRepository.findByDemandePublicIdAndStatusDelFalse(publicId)
                .orElse(null);

        if (facture != null) {
            facture.setMontant(toFactureMontant(updatedDemande.getMontant()));
            factureRepository.save(facture);
        }

        return buildDemandeResponse(updatedDemande);
    }

    @Override
    @CacheEvict(cacheNames = {
            CacheConfig.DASHBOARD_STATS,
            CacheConfig.DASHBOARD_PROGRAMMES,
            CacheConfig.DEMANDE_PAGES
    }, allEntries = true)
    public DemandeResponse updateIntention(UUID publicId, DemandeIntentionRequest request) {
        String intention = IntentionTextUtils.requireValid(request == null ? null : request.intention());

        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
        tenantAccessService.checkParoisseAccess(demande.getParoisse());

        demande.setIntention(intention);
        return buildDemandeResponse(demandeRepository.save(demande));
    }

    @Override
    @CacheEvict(cacheNames = {
            CacheConfig.DASHBOARD_STATS,
            CacheConfig.DASHBOARD_PROGRAMMES,
            CacheConfig.DEMANDE_PAGES
    }, allEntries = true)
    public DemandeResponse updateTypePaiementByCodeSuivie(String codeSuivie, UUID typePaiementPublicId) {
        if (codeSuivie == null || codeSuivie.isBlank()) {
            throw new BusinessRuleException("Le code de suivi est obligatoire");
        }
        if (typePaiementPublicId == null) {
            throw new BusinessRuleException("Le type de paiement est obligatoire");
        }

        String code = BusinessCodeGenerator.normalizeDemandeTrackingCode(codeSuivie);
        Demande demande = demandeRepository.findByCodeSuivieAndStatusDelFalse(code)
                .or(() -> demandeRepository.findByCodeSuivieAndStatusDelFalse(codeSuivie.trim()))
                .orElseThrow(() -> new TrackingIdNotFoundException("Demande introuvable pour ce code de suivi"));

        if (demande.getStatutPaiement() == StatutPaiementEnum.PAYE) {
            throw new BusinessRuleException("Le mode de paiement ne peut plus être modifié : la demande est déjà payée");
        }
        if (demande.getStatutDemande() == StatutDemandeEnum.ANNULEE
                || demande.getStatutDemande() == StatutDemandeEnum.REJETEE) {
            throw new BusinessRuleException("Cette demande n'accepte plus de changement de paiement");
        }

        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(typePaiementPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement introuvable"));

        // Suivi public : uniquement les modes en ligne (pas d'espèces / comptant).
        if (typePaiement.getMode() == ModePaiement.ESPECES) {
            throw new BusinessRuleException(
                    "Le paiement au comptant n'est pas disponible via le suivi en ligne. "
                            + "Choisissez TMoney, Flooz ou carte."
            );
        }

        demande.setTypePaiement(typePaiement);
        // Un changement de mode invalide une session FedaPay en cours.
        if (demande.getStatutPaiement() == StatutPaiementEnum.EN_ATTENTE) {
            demande.setStatutPaiement(StatutPaiementEnum.NON_PAYE);
        }

        return buildDemandeResponse(demandeRepository.save(demande));
    }

    @Override
    @CacheEvict(cacheNames = {
            CacheConfig.DASHBOARD_STATS,
            CacheConfig.DASHBOARD_PROGRAMMES,
            CacheConfig.DEMANDE_PAGES
    }, allEntries = true)
    public DemandeResponse updateValidation(UUID publicId, DemandeValidationRequest request) {
        if (request == null || request.statut() == null) {
            throw new BusinessRuleException("Le statut de validation est obligatoire");
        }
        if (request.statut() == StatutValidationEnum.EN_ATTENTE) {
            throw new BusinessRuleException(
                    "Une validation doit être acceptée ou rejetée"
            );
        }

        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
        tenantAccessService.checkParoisseAccess(demande.getParoisse());

        User validator = tenantAccessService.getCurrentUser();
        demande.setStatutValidation(request.statut());
        demande.setStatutDemande(
                request.statut() == StatutValidationEnum.VALIDEE
                        ? StatutDemandeEnum.VALIDEE
                        : StatutDemandeEnum.REJETEE
        );
        demande.setValidateBy(validator.getFullName());

        return buildDemandeResponse(demandeRepository.save(demande));
    }

    @Override
    @Transactional(readOnly = true)
    public DemandeResponse getByPublicId(UUID publicId) {
        Demande demande = demandeRepository.findByPublicIdWithAssociationsIncludingDeleted(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        tenantAccessService.checkParoisseAccess(demande.getParoisse());

        return buildDemandeResponse(demande);
    }

    @Override
    @Transactional(readOnly = true)
    public DemandeResponse getByCodeSuivie(String codeSuivie) {
        String code = BusinessCodeGenerator.normalizeDemandeTrackingCode(codeSuivie);
        Demande demande = demandeRepository.findByCodeSuivieWithAssociations(code)
                .or(() -> demandeRepository.findByCodeSuivieWithAssociations(
                        codeSuivie == null ? "" : codeSuivie.trim()))
                .orElseThrow(() -> new TrackingIdNotFoundException("Code de suivi introuvable"));

        return buildDemandeResponse(demande);
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingByPhoneResponse findTrackingCodesByPhone(String telephone) {
        Set<String> variants = phoneLookupVariants(telephone);
        if (variants.isEmpty()) {
            return new TrackingByPhoneResponse(List.of(), 0);
        }
        List<String> codes = demandeRepository.findCodesByTelFideleIn(
                variants,
                PageRequest.of(0, 10)
        );
        // Dédupliquer en conservant l'ordre.
        List<String> unique = codes.stream().filter(Objects::nonNull).distinct().toList();
        return new TrackingByPhoneResponse(unique, unique.size());
    }

    private static Set<String> phoneLookupVariants(String raw) {
        if (raw == null) return Set.of();
        String compact = raw.trim().replaceAll("[\\s.\\-()]", "");
        if (compact.length() < 8) return Set.of();
        Set<String> out = new LinkedHashSet<>();
        out.add(compact);
        if (compact.startsWith("00") && compact.length() > 4) {
            out.add("+" + compact.substring(2));
        }
        if (compact.startsWith("+")) {
            out.add(compact.substring(1));
        } else if (compact.matches("\\d{8,15}")) {
            out.add("+" + compact);
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeResponse> getAll() {
        // Le tenantFilter Hibernate restreint déjà les lignes pour les utilisateurs non globaux.
        return buildDemandeResponses(demandeRepository.findAllActiveWithAssociations());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DemandeResponse> getAllPaged(int page, int size, boolean includeDeleted) {
        if (!tenantAccessService.isGlobalUser()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Réservé à l’équipe plateforme (comptable / super admin)"
            );
        }
        if (includeDeleted) {
            tenantAccessService.requireIncludeDeletedDemandes();
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Demande> demandePage = includeDeleted
                ? demandeRepository.findAllForPlatformAudit(pageable)
                : demandeRepository.findActiveForPlatformAudit(pageable);
        return PageResponse.of(
                buildDemandeResponses(demandePage.getContent()),
                safePage,
                safeSize,
                demandePage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeResponse> getByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        tenantAccessService.checkParoisseAccess(paroisse);

        return buildDemandeResponses(demandeRepository.findByParoisseWithAssociations(paroisse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DemandeResponse> getByParoissePaged(UUID paroissePublicId, int page, int size) {
        return getByParoissePaged(paroissePublicId, page, size, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DemandeResponse> getByParoissePaged(
            UUID paroissePublicId,
            int page,
            int size,
            boolean includeDeleted
    ) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        tenantAccessService.checkParoisseAccess(paroisse);

        if (includeDeleted) {
            tenantAccessService.requireIncludeDeletedDemandes();
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        // Les archives restent toujours lues directement : elles sont réservées à l'audit
        // et ne doivent pas partager le cache des données actives.
        if (includeDeleted) {
            return buildParoissePage(paroisse, safePage, safeSize, true);
        }

        Cache cache = cacheManager.getCache(CacheConfig.DEMANDE_PAGES);
        String cacheKey = paroissePublicId + ":" + safePage + ":" + safeSize;
        return cache != null
                ? cache.get(cacheKey, () -> buildParoissePage(paroisse, safePage, safeSize, false))
                : buildParoissePage(paroisse, safePage, safeSize, false);
    }

    private PageResponse<DemandeResponse> buildParoissePage(
            Paroisse paroisse,
            int page,
            int size,
            boolean includeDeleted
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Demande> demandePage = includeDeleted
                ? demandeRepository.findByParoisse(paroisse, pageable)
                : demandeRepository.findByParoisseAndStatusDelFalse(paroisse, pageable);
        List<DemandeResponse> content = buildDemandeResponses(demandePage.getContent());
        return PageResponse.of(content, page, size, demandePage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public DemandeParoisseStatsResponse getParoisseStats(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        // Le contrôle tenant reste exécuté à chaque requête, y compris sur un cache hit.
        tenantAccessService.checkParoisseAccess(paroisse);

        Cache cache = cacheManager.getCache(CacheConfig.DASHBOARD_STATS);
        return cache != null
                ? cache.get(paroissePublicId, () -> buildParoisseStats(paroisse))
                : buildParoisseStats(paroisse);
    }

    private DemandeParoisseStatsResponse buildParoisseStats(Paroisse paroisse) {
        DemandeParoisseStatsProjection stats = demandeRepository.aggregateStatsByParoisse(
                paroisse,
                StatutDemandeEnum.EN_ATTENTE,
                StatutDemandeEnum.VALIDEE
        );

        // Pas de Page ici : Top5 évite le COUNT(*) automatique inutile au dashboard.
        List<Demande> recentesEntities =
                demandeRepository.findTop5ByParoisseAndStatusDelFalseOrderByCreatedAtDesc(paroisse);
        List<Demande> impayeesEntities = listUnpaidApproachingForParoisse(paroisse);

        // Les deux widgets partagent le même enrichissement batch (dates, factures, paiements).
        List<Demande> demandesAEnrichir = new ArrayList<>(recentesEntities);
        Set<Long> demandeIds = recentesEntities.stream()
                .map(Demande::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        impayeesEntities.stream()
                .filter(demande -> demande.getId() == null || demandeIds.add(demande.getId()))
                .forEach(demandesAEnrichir::add);

        Map<UUID, DemandeResponse> responsesByPublicId = buildDemandeResponses(demandesAEnrichir)
                .stream()
                .collect(Collectors.toMap(
                        DemandeResponse::publicId,
                        response -> response,
                        (left, right) -> left
                ));

        List<DemandeResponse> recentes = recentesEntities.stream()
                .map(demande -> responsesByPublicId.get(demande.getPublicId()))
                .filter(Objects::nonNull)
                .toList();
        List<DemandeResponse> impayeesProches = impayeesEntities.stream()
                .map(demande -> responsesByPublicId.get(demande.getPublicId()))
                .filter(Objects::nonNull)
                .toList();

        return new DemandeParoisseStatsResponse(
                stats.getTotal() != null ? stats.getTotal() : 0L,
                stats.getEnAttente() != null ? stats.getEnAttente() : 0L,
                stats.getValidees() != null ? stats.getValidees() : 0L,
                stats.getVolumeMontant() != null ? stats.getVolumeMontant() : BigDecimal.ZERO,
                recentes,
                impayeesProches.size(),
                impayeesProches
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeResponse> getByParoisseAndStatut(UUID paroissePublicId, StatutDemandeEnum statutDemande) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        tenantAccessService.checkParoisseAccess(paroisse);

        return buildDemandeResponses(
                demandeRepository.findByParoisseAndStatutWithAssociations(paroisse, statutDemande)
        );
    }

    @Override
    @CacheEvict(cacheNames = {
            CacheConfig.DASHBOARD_STATS,
            CacheConfig.DASHBOARD_PROGRAMMES,
            CacheConfig.DEMANDE_PAGES
    }, allEntries = true)
    public void deleteByPublicId(UUID publicId) {
        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        tenantAccessService.checkParoisseAccess(demande.getParoisse());

        softDeleteRelatedEntities(demande);

        User actor = tenantAccessService.getCurrentUser();
        cancelDemande(demande, actor != null ? actor.getFullName() : "Système");
    }

    /**
     * Politique produit : rappel e-mail dès J-3 (toutes les 6 h), puis
     * annulation automatique si toujours impayé à l'approche (H-6).
     */
    @Override
    @CacheEvict(cacheNames = {
            CacheConfig.DASHBOARD_STATS,
            CacheConfig.DASHBOARD_PROGRAMMES,
            CacheConfig.DEMANDE_PAGES
    }, allEntries = true)
    public int cancelUnpaidApproachingCelebrations() {
        if (!demandePaymentProperties.isUnpaidAutoCancelEnabled()) {
            return 0;
        }

        int hoursBefore = Math.max(0, demandePaymentProperties.getUnpaidCancelHoursBefore());
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime threshold = now.plusHours(hoursBefore);
        LocalDate maxDate = threshold.toLocalDate();

        List<DemandeDate> candidates = demandeDateRepository.findFirstCelebrationsUnpaidForCancel(
                maxDate,
                StatutPaiementEnum.PAYE,
                EnumSet.of(StatutDemandeEnum.EN_ATTENTE, StatutDemandeEnum.VALIDEE)
        );

        int cancelled = 0;
        for (DemandeDate firstDate : candidates) {
            Demande demande = firstDate.getDemande();
            if (demande == null || Boolean.TRUE.equals(demande.getStatusDel())) {
                continue;
            }
            if (demande.getStatutPaiement() == StatutPaiementEnum.PAYE) {
                continue;
            }

            LocalDateTime celebrationAt = resolveCelebrationAt(demande, firstDate);

            // Annule si la célébration est dans la fenêtre (ou déjà commencée).
            if (!celebrationAt.isAfter(threshold)) {
                cancelDemande(demande, SYSTEM_UNPAID_CANCEL_ACTOR);
                cancelled++;
                log.info(
                        "Demande {} annulée (impayé, 1ère célébration {})",
                        demande.getCodeSuivie(),
                        celebrationAt
                );
            }
        }
        return cancelled;
    }

    @Override
    public int remindUnpaidApproachingCelebrations() {
        if (!demandePaymentProperties.isUnpaidReminderEnabled()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        int daysBefore = Math.max(1, demandePaymentProperties.getUnpaidReminderDaysBefore());
        LocalDateTime windowEnd = now.plusDays(daysBefore);
        LocalDate minDate = now.toLocalDate();
        LocalDate maxDate = windowEnd.toLocalDate();

        List<DemandeDate> candidates = demandeDateRepository.findFirstCelebrationsUnpaidForReminder(
                minDate,
                maxDate,
                StatutPaiementEnum.PAYE,
                EnumSet.of(StatutDemandeEnum.EN_ATTENTE, StatutDemandeEnum.VALIDEE)
        );

        int sent = 0;
        for (DemandeDate firstDate : candidates) {
            Demande demande = firstDate.getDemande();
            if (demande == null || Boolean.TRUE.equals(demande.getStatusDel())) {
                continue;
            }
            if (demande.getStatutPaiement() == StatutPaiementEnum.PAYE) {
                continue;
            }
            if (!StringUtils.hasText(demande.getEmailFidele())) {
                continue;
            }

            LocalDateTime celebrationAt = resolveCelebrationAt(demande, firstDate);
            if (celebrationAt.isBefore(now) || celebrationAt.isAfter(windowEnd)) {
                continue;
            }

            LocalDateTime last = demande.getLastUnpaidReminderAt();
            if (last != null && ChronoUnit.HOURS.between(last, now) < REMINDER_MIN_HOURS) {
                continue;
            }

            sendUnpaidReminderEmail(demande, celebrationAt);
            demande.setLastUnpaidReminderAt(now);
            demandeRepository.save(demande);
            sent++;
        }
        return sent;
    }

    private List<Demande> listUnpaidApproachingForParoisse(Paroisse paroisse) {
        LocalDateTime now = LocalDateTime.now(clock);
        int daysBefore = Math.max(1, demandePaymentProperties.getUnpaidReminderDaysBefore());
        LocalDateTime windowEnd = now.plusDays(daysBefore);

        List<DemandeDate> rows = demandeDateRepository.findFirstCelebrationsUnpaidForReminderByParoisse(
                paroisse,
                now.toLocalDate(),
                windowEnd.toLocalDate(),
                StatutPaiementEnum.PAYE,
                EnumSet.of(StatutDemandeEnum.EN_ATTENTE, StatutDemandeEnum.VALIDEE)
        );

        List<Demande> result = new ArrayList<>();
        for (DemandeDate firstDate : rows) {
            Demande demande = firstDate.getDemande();
            if (demande == null || Boolean.TRUE.equals(demande.getStatusDel())) {
                continue;
            }
            LocalDateTime celebrationAt = resolveCelebrationAt(demande, firstDate);
            if (!celebrationAt.isBefore(now) && !celebrationAt.isAfter(windowEnd)) {
                result.add(demande);
            }
        }
        return result;
    }

    private LocalDateTime resolveCelebrationAt(Demande demande, DemandeDate firstDate) {
        LocalTime celebrationTime = demande.getHeurePersonnalisee() != null
                ? demande.getHeurePersonnalisee()
                : (demande.getHoraire() != null
                        ? demande.getHoraire().getHeureCelebration()
                        : LocalTime.MIDNIGHT);
        return LocalDateTime.of(firstDate.getDateCelebration(), celebrationTime);
    }

    private void sendUnpaidReminderEmail(Demande demande, LocalDateTime celebrationAt) {
        String base = StringUtils.hasText(demandePaymentProperties.getPublicBaseUrl())
                ? demandePaymentProperties.getPublicBaseUrl().replaceAll("/+$", "")
                : "http://localhost:5173";
        String suiviUrl = base + "/suivi/resultat?code=" + demande.getCodeSuivie();
        String paroisseNom = demande.getParoisse() != null ? demande.getParoisse().getNom() : "votre paroisse";
        String fidel = FideleNameUtils.format(demande.getPrenomFidele(), demande.getNomFidele());

        String subject = "Rappel : paiement en attente — " + demande.getCodeSuivie();
        String body = """
                Bonjour %s,

                Votre demande d'intention à %s n'est pas encore réglée.
                Code de suivi : %s
                Première célébration : %s à %s
                Montant : %s FCFA

                Sans paiement, la demande pourra être annulée automatiquement
                à l'approche de la célébration.

                Suivre / payer : %s

                — Messes Archidiocèse de Lomé
                """.formatted(
                fidel,
                paroisseNom,
                demande.getCodeSuivie(),
                celebrationAt.toLocalDate().format(REMINDER_DATE_FORMAT),
                celebrationAt.toLocalTime().format(REMINDER_TIME_FORMAT),
                demande.getMontant() != null ? demande.getMontant().toPlainString() : "—",
                suiviUrl
        );

        appMailService.sendText(demande.getEmailFidele().trim(), subject, body);
        log.info("Rappel impayé envoyé pour {} → {}", demande.getCodeSuivie(), demande.getEmailFidele());
    }

    private void cancelDemande(Demande demande, String deletedByNom) {
        softDeleteRelatedEntities(demande);
        demande.setStatusDel(true);
        demande.setStatutDemande(StatutDemandeEnum.ANNULEE);
        demande.setDeletedAt(LocalDateTime.now(clock));
        demande.setDeletedByNom(deletedByNom);
        demandeRepository.save(demande);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeResponse> getDeletedByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        tenantAccessService.checkParoisseAccess(paroisse);
        return buildDemandeResponses(demandeRepository.findDeletedByParoisseWithAssociations(paroisse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeResponse> getByTypePaiement(UUID typePaiementPublicId) {
        return buildDemandeResponses(
                demandeRepository.findByTypePaiementWithAssociations(typePaiementPublicId)
        );
    }

    private void validateTypeDemandeParoisse(TypeDemande typeDemande, Paroisse paroisse) {
        UUID typeParoissePublicId = typeDemande.getParoisse() != null
                ? typeDemande.getParoisse().getPublicId()
                : null;

        if (!Objects.equals(typeParoissePublicId, paroisse.getPublicId())) {
            throw new BusinessRuleException(
                    "Le type de demande ne correspond pas à la paroisse choisie"
            );
        }
    }

    /**
     * Nom / prénom optionnels :
     * prénom seul, nom seul, les deux, ou rien → {@link FideleNameUtils#DEFAULT}.
     */
    private void applyFideleIdentity(Demande demande, DemandeRequest request) {
        String nom = normalizeOptionalPersonName(request.nomFidele(), "nom");
        String prenom = normalizeOptionalPersonName(request.prenomFidele(), "prénom");

        if (nom == null && prenom == null) {
            demande.setPrenomFidele(FideleNameUtils.DEFAULT);
            demande.setNomFidele("");
        } else if (nom == null) {
            demande.setPrenomFidele(prenom);
            demande.setNomFidele("");
        } else if (prenom == null) {
            demande.setPrenomFidele("");
            demande.setNomFidele(nom);
        } else {
            demande.setPrenomFidele(prenom);
            demande.setNomFidele(nom);
        }
    }

    /** @return valeur normalisée, ou {@code null} si vide */
    private String normalizeOptionalPersonName(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        // Apostrophes typographiques (’ ‘ ʻ ` ´) → ASCII pour O'Brien, D'Almeida, etc.
        String trimmed = raw.trim()
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u02BC', '\'')
                .replace('`', '\'')
                .replace('\u00B4', '\'')
                .replaceAll("\\s+", " ");
        if (!PERSON_NAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Le " + label + " du fidèle ne doit contenir que des lettres "
                            + "(espaces, tirets et apostrophes autorisés)."
            );
        }
        return trimmed;
    }

    private void requireActive(boolean active, String message) {
        if (!active) {
            throw new BusinessRuleException(message);
        }
    }

    private void applyInitialStatuses(Demande demande, ForfaitTarif forfaitTarif) {
        applyValidationStatuses(demande, forfaitTarif);
        demande.setStatutPaiement(StatutPaiementEnum.NON_PAYE);
    }

    private void applyValidationStatuses(Demande demande, ForfaitTarif forfaitTarif) {
        boolean isSpecialRequest = forfaitTarif.getNatureForfait() == NatureForfaitEnum.SPECIALE;

        if (isSpecialRequest) {
            demande.setStatutValidation(StatutValidationEnum.EN_ATTENTE);
            demande.setStatutDemande(StatutDemandeEnum.EN_ATTENTE);
            demande.setValidateBy(null);
        } else {
            demande.setStatutValidation(StatutValidationEnum.VALIDEE);
            demande.setStatutDemande(StatutDemandeEnum.VALIDEE);
        }
    }

    /**
     * Demande spéciale : téléphone et e-mail valides obligatoires (contact pour validation).
     */
    private void validateSpecialeContact(DemandeRequest request, ForfaitTarif forfaitTarif) {
        if (forfaitTarif == null || forfaitTarif.getNatureForfait() != NatureForfaitEnum.SPECIALE) {
            return;
        }
        if (!StringUtils.hasText(request.telFidele())) {
            throw new BusinessRuleException(
                    "Pour une messe spéciale, un numéro de téléphone valide est obligatoire."
            );
        }
        String email = request.emailFidele() != null ? request.emailFidele().trim() : "";
        if (!StringUtils.hasText(email)) {
            throw new BusinessRuleException(
                    "Pour une messe spéciale, une adresse e-mail valide est obligatoire."
            );
        }
        if (!email.contains("@") || email.length() > 150) {
            throw new BusinessRuleException(
                    "Pour une messe spéciale, l'adresse e-mail fournie n'est pas valide."
            );
        }
    }

    private void softDeleteRelatedEntities(Demande demande) {
        List<DemandeDate> dates = demandeDateRepository.findByDemande_IdAndStatusDelFalse(demande.getId());
        if (!dates.isEmpty()) {
            dates.forEach(date -> date.setStatusDel(true));
            demandeDateRepository.saveAll(dates);
        }

        Facture facture = factureRepository.findByDemandePublicIdAndStatusDelFalse(demande.getPublicId())
                .orElse(null);
        if (facture != null) {
            detailsPaiementRepository.findByFacturePublicIdAndStatusDelFalse(facture.getPublicId())
                    .ifPresent(details -> {
                        details.setStatusDel(true);
                        detailsPaiementRepository.save(details);
                    });
            facture.setStatusDel(true);
            factureRepository.save(facture);
        }
    }

    private int toFactureMontant(java.math.BigDecimal montant) {
        if (montant == null) {
            throw new BusinessRuleException("Le montant de la demande est obligatoire");
        }
        return montant.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
    }

    private void validateDates(DemandeRequest request, ForfaitTarif forfaitTarif) {
        Integer nombreCelebrations = forfaitTarif.getNombreCelebration();

        if (nombreCelebrations == null || nombreCelebrations <= 0) {
            throw new IllegalArgumentException("Le forfait ne définit pas un nombre valide de célébrations");
        }

        boolean hasUserDates = request.datesCelebration() != null
                && request.datesCelebration().stream().anyMatch(Objects::nonNull);
        boolean hasStart = request.dateDebut() != null;

        if (ForfaitDureeLabels.isMultiCelebration(nombreCelebrations)) {
            // Triduum / neuvaine / trentaine : date de début suffit (les N dates sont dérivées).
            if (!hasStart && !hasUserDates) {
                throw new BusinessRuleException(
                        "La date de début du "
                                + ForfaitDureeLabels.labelFor(nombreCelebrations)
                                + " est obligatoire"
                );
            }
            return;
        }

        if (!hasUserDates && !hasStart) {
            throw new BusinessRuleException("La date de célébration est obligatoire");
        }
    }

    private void validateCelebrationSchedule(
            DemandeRequest request,
            Horaire horaire,
            TypeDemande typeDemande,
            ForfaitTarif forfaitTarif
    ) {
        Set<JourSemaine> allowedDays = demandeSchedulingPolicy.resolveAllowedDays(
                typeDemande.getJoursCelebrationAutorises(),
                forfaitTarif.getJoursCelebrationAutorises()
        );

        List<LocalDate> celebrationDates = resolveCelebrationDates(request, typeDemande, forfaitTarif, allowedDays);

        LocalTime celebrationTime = request.heurePersonnalisee() != null
                ? request.heurePersonnalisee()
                : horaire != null ? horaire.getHeureCelebration() : null;

        // Pour une seule célébration avec horaire fixe : la date doit coller au jour de l'horaire.
        // Pour un multi-jours (dates libres), l'horaire ne fournit que l'heure.
        if (horaire != null && celebrationDates.size() == 1) {
            demandeSchedulingPolicy.validateHoraireDay(celebrationDates.get(0), horaire.getJourSemaine());
            horaireService.assertHoraireAllowedOnDate(horaire, celebrationDates.get(0));
            assertNatureHonoraireMatches(horaire, forfaitTarif);
        }

        for (LocalDate date : celebrationDates) {
            // Célébration unique sur date précise / messe unique.
            horaireService.assertUniqueMassSlot(
                    request.paroissePublicId(),
                    date,
                    horaire,
                    request.heurePersonnalisee()
            );
            demandeSchedulingPolicy.validate(
                    date,
                    celebrationTime,
                    typeDemande.getDelaiMinimumHeures()
            );
        }
    }

    private boolean hasCelebrationSlots(DemandeRequest request) {
        return request.celebrationSlots() != null
                && request.celebrationSlots().stream()
                .anyMatch(slot -> slot != null && slot.date() != null);
    }

    private void validateCelebrationSlots(
            DemandeRequest request,
            Paroisse paroisse,
            TypeDemande typeDemande,
            ForfaitTarif forfaitTarif
    ) {
        Set<JourSemaine> allowedDays = demandeSchedulingPolicy.resolveAllowedDays(
                typeDemande.getJoursCelebrationAutorises(),
                forfaitTarif.getJoursCelebrationAutorises()
        );
        List<LocalDate> celebrationDates = resolveCelebrationDates(request, typeDemande, forfaitTarif, allowedDays);
        String label = ForfaitDureeLabels.labelFor(forfaitTarif.getNombreCelebration());
        boolean hp = Boolean.TRUE.equals(forfaitTarif.getHeurePersonnalise());

        Map<LocalDate, CelebrationSlotRequest> byDate = request.celebrationSlots().stream()
                .filter(slot -> slot != null && slot.date() != null)
                .collect(Collectors.toMap(
                        CelebrationSlotRequest::date,
                        slot -> slot,
                        (a, b) -> a
                ));

        if (byDate.size() != celebrationDates.size()
                || !byDate.keySet().containsAll(celebrationDates)) {
            throw new BusinessRuleException(
                    "Chaque date du " + label + " doit avoir un créneau de célébration"
            );
        }

        for (LocalDate date : celebrationDates) {
            CelebrationSlotRequest slot = byDate.get(date);
            Horaire slotHoraire = null;
            if (slot.horairePublicId() != null) {
                slotHoraire = horaireRepository.findByPublicIdAndStatusDelFalse(slot.horairePublicId())
                        .orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable"));
                requireActive(Boolean.TRUE.equals(slotHoraire.getIsActive()), "Cet horaire n'est plus disponible");
                if (!slotHoraire.getParoisse().getPublicId().equals(paroisse.getPublicId())) {
                    throw new IllegalArgumentException("L'horaire ne correspond pas à la paroisse choisie");
                }
                demandeSchedulingPolicy.validateHoraireDay(date, slotHoraire.getJourSemaine());
                horaireService.assertHoraireAllowedOnDate(slotHoraire, date);
                assertNatureHonoraireMatches(slotHoraire, forfaitTarif);
            }

            LocalTime heurePerso = slot.heurePersonnalisee();
            horaireService.assertUniqueMassSlot(
                    paroisse.getPublicId(),
                    date,
                    slotHoraire,
                    heurePerso
            );

            if (hp) {
                if (heurePerso == null && slotHoraire == null) {
                    throw new BusinessRuleException(
                            "Indiquez un horaire ou une heure personnalisée pour le " + date
                    );
                }
            } else {
                if (slotHoraire == null) {
                    throw new BusinessRuleException(
                            "Un horaire paroissial est obligatoire pour le " + date
                    );
                }
                if (heurePerso != null) {
                    throw new BusinessRuleException(
                            "L'heure personnalisée n'est pas autorisée pour ce forfait"
                    );
                }
            }

            LocalTime celebrationTime = heurePerso != null
                    ? heurePerso
                    : slotHoraire.getHeureCelebration();
            demandeSchedulingPolicy.validate(date, celebrationTime, typeDemande.getDelaiMinimumHeures());
        }
    }

    private Horaire resolveFirstSlotHoraire(DemandeRequest request, Paroisse paroisse) {
        if (!hasCelebrationSlots(request)) {
            return null;
        }
        return request.celebrationSlots().stream()
                .filter(slot -> slot != null && slot.horairePublicId() != null)
                .findFirst()
                .map(slot -> {
                    Horaire h = horaireRepository.findByPublicIdAndStatusDelFalse(slot.horairePublicId())
                            .orElse(null);
                    if (h != null && !h.getParoisse().getPublicId().equals(paroisse.getPublicId())) {
                        return null;
                    }
                    return h;
                })
                .orElse(null);
    }

    private List<LocalDate> resolveCelebrationDates(
            DemandeRequest request,
            TypeDemande typeDemande,
            ForfaitTarif forfaitTarif,
            Set<JourSemaine> allowedDays
    ) {
        int nombreCelebrations = forfaitTarif.getNombreCelebration();
        String label = ForfaitDureeLabels.labelFor(nombreCelebrations);
        boolean multi = ForfaitDureeLabels.isMultiCelebration(nombreCelebrations);

        long providedCount = request.datesCelebration() == null
                ? 0
                : request.datesCelebration().stream().filter(Objects::nonNull).distinct().count();

        if (multi) {
            // Si le client envoie déjà exactement N dates, on les valide telles quelles.
            if (providedCount == nombreCelebrations) {
                return demandeSchedulingPolicy.validateAndNormalizeUserDates(
                        request.datesCelebration(),
                        nombreCelebrations,
                        forfaitTarif.getNombreJour(),
                        allowedDays,
                        label
                );
            }

            LocalDate start = request.dateDebut();
            if (start == null && providedCount > 0) {
                start = request.datesCelebration().stream()
                        .filter(Objects::nonNull)
                        .min(LocalDate::compareTo)
                        .orElse(null);
            }
            if (start == null) {
                throw new BusinessRuleException("La date de début du " + label + " est obligatoire");
            }

            List<LocalDate> computed = demandeSchedulingPolicy.computeCelebrationDates(
                    start,
                    allowedDays,
                    nombreCelebrations
            );
            return demandeSchedulingPolicy.validateAndNormalizeUserDates(
                    computed,
                    nombreCelebrations,
                    forfaitTarif.getNombreJour(),
                    allowedDays,
                    label
            );
        }

        LocalDate start = request.dateDebut();
        if (start == null && providedCount > 0) {
            start = request.datesCelebration().stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        if (start == null) {
            throw new BusinessRuleException("La date de célébration est obligatoire");
        }
        // Événement solennel / date précise du programme : n'importe quel jour,
        // l'honoraire est porté par le créneau (natureHonoraire), pas par la grille hebdo.
        boolean programmeDatePrecis = request.paroissePublicId() != null
                && horaireService.isDateSpecifiqueProgramme(request.paroissePublicId(), start);
        if (!programmeDatePrecis) {
            demandeSchedulingPolicy.validateAllowedDay(start, allowedDays, "Ce " + label);
        }
        return List.of(start);
    }

    private void assertNatureHonoraireMatches(Horaire horaire, ForfaitTarif forfaitTarif) {
        if (horaire == null || forfaitTarif == null || horaire.getNatureHonoraire() == null) {
            return;
        }
        NatureForfaitEnum expected = horaire.getNatureHonoraire();
        NatureForfaitEnum actual = forfaitTarif.getNatureForfait();
        if (actual != null && actual != expected) {
            throw new BusinessRuleException(
                    "Pour cette célébration du programme, l'honoraire applicable est « "
                            + expected.name()
                            + " » (forfait choisi : "
                            + actual.name()
                            + ")"
            );
        }
    }

    /**
     * Aligne le forfait (et donc le montant) sur le jour de célébration :
     * dimanche → DOMINICALE, semaine → NORMALE, créneau SPECIALE → SPECIALE.
     * Les formules multi (triduum / neuvaine…) et le choix explicite SPECIALE sont conservés.
     */
    private ForfaitTarif resolveForfaitForCelebrationDay(
            TypeDemande typeDemande,
            ForfaitTarif requested,
            DemandeRequest request,
            Horaire horaire
    ) {
        if (requested == null || typeDemande == null) {
            return requested;
        }
        if (ForfaitDureeLabels.isMultiCelebration(requested.getNombreCelebration())) {
            return requested;
        }
        // SPECIALE volontaire (hors créneau imposé) : on respecte le choix du fidèle.
        if (requested.getNatureForfait() == NatureForfaitEnum.SPECIALE
                && (horaire == null || horaire.getNatureHonoraire() == null
                || horaire.getNatureHonoraire() == NatureForfaitEnum.SPECIALE)) {
            return requested;
        }

        LocalDate celebrationDate = resolvePrimaryCelebrationDate(request);
        if (celebrationDate == null && horaire == null) {
            return requested;
        }

        NatureForfaitEnum required = resolveRequiredNature(celebrationDate, horaire);
        if (required == null || required == requested.getNatureForfait()) {
            return requested;
        }

        Integer nombre = requested.getNombreCelebration();
        ForfaitTarif matched = forfaitTarifRepository
                .findByTypeDemandeAndIsActiveTrueAndStatusDelFalse(typeDemande)
                .stream()
                .filter(f -> f.getNatureForfait() == required)
                .filter(f -> Objects.equals(f.getNombreCelebration(), nombre))
                .filter(f -> celebrationDate == null || forfaitAllowsDay(f, celebrationDate))
                .findFirst()
                .orElse(null);

        if (matched == null) {
            throw new BusinessRuleException(
                    "Le tarif « " + required.name()
                            + " » est requis pour cette date de célébration. "
                            + "Aucun forfait actif correspondant n’est configuré pour cette formule."
            );
        }

        log.info(
                "Forfait aligné sur le jour de célébration : {} → {} (montant {})",
                requested.getNatureForfait(),
                matched.getNatureForfait(),
                matched.getMontantForfait()
        );
        return matched;
    }

    private static NatureForfaitEnum resolveRequiredNature(LocalDate celebrationDate, Horaire horaire) {
        if (horaire != null && horaire.getNatureHonoraire() != null) {
            return horaire.getNatureHonoraire();
        }
        if (celebrationDate == null) {
            return null;
        }
        JourSemaine jour = JourSemaine.fromDayOfWeek(celebrationDate.getDayOfWeek());
        return jour == JourSemaine.DIMANCHE ? NatureForfaitEnum.DOMINICALE : NatureForfaitEnum.NORMALE;
    }

    private static boolean forfaitAllowsDay(ForfaitTarif forfait, LocalDate date) {
        Set<JourSemaine> days = forfait.getJoursCelebrationAutorises();
        if (days == null || days.isEmpty()) {
            return true;
        }
        return days.contains(JourSemaine.fromDayOfWeek(date.getDayOfWeek()));
    }

    private LocalDate resolvePrimaryCelebrationDate(DemandeRequest request) {
        if (request.dateDebut() != null) {
            return request.dateDebut();
        }
        if (request.datesCelebration() != null) {
            return request.datesCelebration().stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        if (hasCelebrationSlots(request)) {
            return request.celebrationSlots().stream()
                    .filter(slot -> slot != null && slot.date() != null)
                    .map(CelebrationSlotRequest::date)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private void generateDemandeDates(
            Demande demande,
            DemandeRequest request,
            TypeDemande typeDemande,
            ForfaitTarif forfaitTarif
    ) {
        Integer nombreCelebrations = forfaitTarif.getNombreCelebration();

        if (nombreCelebrations == null || nombreCelebrations <= 0) {
            return;
        }

        Set<JourSemaine> allowedDays = demandeSchedulingPolicy.resolveAllowedDays(
                typeDemande.getJoursCelebrationAutorises(),
                forfaitTarif.getJoursCelebrationAutorises()
        );

        List<LocalDate> celebrationDates = resolveCelebrationDates(request, typeDemande, forfaitTarif, allowedDays);

        Map<LocalDate, CelebrationSlotRequest> slotsByDate = hasCelebrationSlots(request)
                ? request.celebrationSlots().stream()
                .filter(slot -> slot != null && slot.date() != null)
                .collect(Collectors.toMap(CelebrationSlotRequest::date, slot -> slot, (a, b) -> a))
                : Map.of();

        List<DemandeDate> demandeDates = new ArrayList<>();

        for (int i = 0; i < celebrationDates.size(); i++) {
            LocalDate date = celebrationDates.get(i);
            DemandeDate demandeDate = new DemandeDate();
            demandeDate.setDemande(demande);
            demandeDate.setOrdre(i + 1);
            demandeDate.setDateCelebration(date);

            CelebrationSlotRequest slot = slotsByDate.get(date);
            if (slot != null) {
                if (slot.horairePublicId() != null) {
                    Horaire slotHoraire = horaireRepository.findByPublicIdAndStatusDelFalse(slot.horairePublicId())
                            .orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable"));
                    demandeDate.setHoraire(slotHoraire);
                }
                demandeDate.setHeurePersonnalisee(slot.heurePersonnalisee());
            } else {
                // Compat : une seule heure / horaire au niveau demande.
                demandeDate.setHoraire(demande.getHoraire());
                demandeDate.setHeurePersonnalisee(demande.getHeurePersonnalisee());
            }
            demandeDates.add(demandeDate);
        }

        demandeDateRepository.saveAll(demandeDates);
    }

    private void refreshDemandeDates(
            Demande demande,
            DemandeRequest request,
            TypeDemande typeDemande,
            ForfaitTarif forfaitTarif
    ) {
        List<DemandeDate> anciennesDates =
                demandeDateRepository.findByDemande_IdAndStatusDelFalse(demande.getId());

        if (!anciennesDates.isEmpty()) {
            anciennesDates.forEach(item -> item.setStatusDel(true));
            demandeDateRepository.saveAll(anciennesDates);
        }

        generateDemandeDates(demande, request, typeDemande, forfaitTarif);
    }

    private DemandeResponse buildDemandeResponse(Demande demande) {
        return buildDemandeResponses(List.of(demande)).get(0);
    }

    /**
     * Enrichit une liste de demandes en 3 requêtes batch (dates, factures, détails)
     * au lieu de 3×N requêtes individuelles.
     */
    private List<DemandeResponse> buildDemandeResponses(List<Demande> demandes) {
        if (demandes == null || demandes.isEmpty()) {
            return List.of();
        }

        List<Long> demandeIds = demandes.stream().map(Demande::getId).filter(Objects::nonNull).toList();
        List<UUID> demandePublicIds = demandes.stream().map(Demande::getPublicId).toList();

        Map<Long, List<DemandeDate>> datesEntityByDemandeId = demandeIds.isEmpty()
                ? Map.of()
                : demandeDateRepository.findWithHoraireByDemandeIds(demandeIds)
                .stream()
                .collect(Collectors.groupingBy(dd -> dd.getDemande().getId()));

        List<Facture> factures = demandePublicIds.isEmpty()
                ? List.of()
                : factureRepository.findByDemande_PublicIdInAndStatusDelFalse(demandePublicIds);

        Map<UUID, Facture> factureByDemandePublicId = factures.stream()
                .filter(f -> f.getDemande() != null)
                .collect(Collectors.toMap(
                        f -> f.getDemande().getPublicId(),
                        f -> f,
                        (left, right) -> left
                ));

        List<UUID> facturePublicIds = factures.stream().map(Facture::getPublicId).toList();
        Map<UUID, DetailsPaiement> detailsByFacturePublicId = facturePublicIds.isEmpty()
                ? Map.of()
                : detailsPaiementRepository.findByFacture_PublicIdInAndStatusDelFalse(facturePublicIds)
                .stream()
                .filter(d -> d.getFacture() != null)
                .collect(Collectors.toMap(
                        d -> d.getFacture().getPublicId(),
                        d -> d,
                        (left, right) -> left
                ));

        return demandes.stream()
                .map(demande -> {
                    DemandeResponse base = demandeMapper.modelToDto(demande);
                    Facture facture = factureByDemandePublicId.get(demande.getPublicId());
                    UUID facturePublicId = null;
                    String refFacture = null;
                    LocalDateTime dateDetailsPaiement = null;
                    String idTransaction = null;
                    String numero = null;

                    if (facture != null) {
                        facturePublicId = facture.getPublicId();
                        refFacture = facture.getRefFacture();
                        DetailsPaiement details = detailsByFacturePublicId.get(facture.getPublicId());
                        if (details != null) {
                            dateDetailsPaiement = details.getDateDetailsPaiement();
                            idTransaction = details.getIdTransaction();
                            numero = details.getNumero();
                        }
                    }

                    List<DemandeDate> dateRows = datesEntityByDemandeId.getOrDefault(demande.getId(), List.of())
                            .stream()
                            .sorted(Comparator.comparing(
                                    DemandeDate::getOrdre,
                                    Comparator.nullsLast(Integer::compareTo)
                            ))
                            .toList();
                    List<LocalDate> datesCelebration = dateRows.stream()
                            .map(DemandeDate::getDateCelebration)
                            .filter(Objects::nonNull)
                            .toList();
                    List<CelebrationSlotResponse> celebrationSlots = dateRows.stream()
                            .map(this::toCelebrationSlot)
                            .toList();

                    return new DemandeResponse(
                            base.publicId(),
                            base.intention(),
                            base.codeSuivie(),
                            base.nomFidele(),
                            base.prenomFidele(),
                            base.telFidele(),
                            base.emailFidele(),
                            base.montant(),
                            base.nomCoursier(),
                            base.statutPaiement(),
                            base.statutValidation(),
                            base.validateBy(),
                            base.heurePersonnalisee(),
                            base.statutDemande(),
                            base.paroissePublicId(),
                            base.paroisseNom(),
                            base.typeDemandePublicId(),
                            base.typeDemandeLibelle(),
                            base.forfaitTarifPublicId(),
                            base.forfaitTarifNom(),
                            base.horairePublicId(),
                            base.horaireLibelle(),
                            base.horaireHeure(),
                            base.userPublicId(),
                            base.username(),
                            base.typePaiementPublicId(),
                            base.typePaiementLibelle(),
                            base.modePaiement(),
                            base.statusDel(),
                            base.deletedAt(),
                            base.deletedByNom(),
                            base.createdAt(),
                            base.updatedAt(),
                            datesCelebration,
                            celebrationSlots,
                            facturePublicId,
                            refFacture,
                            dateDetailsPaiement,
                            idTransaction,
                            numero
                    );
                })
                .toList();
    }

    private CelebrationSlotResponse toCelebrationSlot(DemandeDate dd) {
        LocalTime heure = dd.getHeurePersonnalisee();
        String libelle = null;
        UUID horaireId = null;
        if (dd.getHoraire() != null) {
            horaireId = dd.getHoraire().getPublicId();
            libelle = dd.getHoraire().getLibelle();
            if (heure == null) {
                heure = dd.getHoraire().getHeureCelebration();
            }
        }
        if (heure == null && dd.getDemande() != null) {
            if (dd.getDemande().getHeurePersonnalisee() != null) {
                heure = dd.getDemande().getHeurePersonnalisee();
            } else if (dd.getDemande().getHoraire() != null) {
                heure = dd.getDemande().getHoraire().getHeureCelebration();
                if (libelle == null) {
                    libelle = dd.getDemande().getHoraire().getLibelle();
                }
                if (horaireId == null) {
                    horaireId = dd.getDemande().getHoraire().getPublicId();
                }
            }
        }
        return new CelebrationSlotResponse(
                dd.getDateCelebration(),
                dd.getOrdre(),
                heure,
                libelle,
                horaireId
        );
    }

    private void validateHoraire(LocalTime heurePersonnalisee, Horaire horaire, ForfaitTarif forfaitTarif) {
        if (Boolean.TRUE.equals(forfaitTarif.getHeurePersonnalise())) {
            if (heurePersonnalisee == null && horaire == null) {
                throw new IllegalArgumentException("Une heure personnalisée ou un horaire doit être renseigné");
            }
        } else {
            if (horaire == null) {
                throw new IllegalArgumentException("Un horaire fixe est obligatoire pour ce forfait");
            }
            if (heurePersonnalisee != null) {
                throw new IllegalArgumentException("L'heure personnalisée n'est pas autorisée pour ce forfait");
            }
        }
    }

    private String generateTrackingCode(Paroisse paroisse) {
        String parishName = paroisse != null ? paroisse.getNom() : null;
        String code = BusinessCodeGenerator.unique(
                BusinessCodeGenerator.demandeCode(parishName),
                demandeRepository::existsByCodeSuivieAndStatusDelFalse
        );
        log.debug("Code de suivi généré pour « {} » : {}", parishName, code);
        return code;
    }

    private String generateFactureReference(Paroisse paroisse) {
        String parishName = paroisse != null ? paroisse.getNom() : null;
        return BusinessCodeGenerator.unique(
                BusinessCodeGenerator.factureCode(parishName),
                ref -> factureRepository.findByRefFactureAndStatusDelFalse(ref).isPresent()
        );
    }
}
