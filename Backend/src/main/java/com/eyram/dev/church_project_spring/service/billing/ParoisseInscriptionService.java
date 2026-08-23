package com.eyram.dev.church_project_spring.service.billing;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseInscriptionRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseInscriptionResponse;
import com.eyram.dev.church_project_spring.DTO.response.PlanSaasResponse;
import com.eyram.dev.church_project_spring.entities.Doyenne;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.ParoisseInscription;
import com.eyram.dev.church_project_spring.entities.ParoisseInscriptionMembre;
import com.eyram.dev.church_project_spring.entities.PlanSaas;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import com.eyram.dev.church_project_spring.enums.StatutInscription;
import com.eyram.dev.church_project_spring.enums.StatutTenant;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.DoyenneRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseInscriptionRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.service.PlanSaasService;
import com.eyram.dev.church_project_spring.service.ProfessionalEmailService;
import com.eyram.dev.church_project_spring.service.accounting.ParishLedgerService;
import com.eyram.dev.church_project_spring.service.mail.AppMailService;
import com.eyram.dev.church_project_spring.service.storage.StoredFileService;
import com.eyram.dev.church_project_spring.service.tenant.TenantCatalogBootstrapService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParoisseInscriptionService {

    private final ParoisseInscriptionRepository inscriptionRepository;
    private final DoyenneRepository doyenneRepository;
    private final ParoisseRepository paroisseRepository;
    private final UserRepository userRepository;
    private final ParoisseAccessRepository paroisseAccessRepository;
    private final PasswordEncoder passwordEncoder;
    private final ParishLedgerService parishLedgerService;
    private final SubscriptionBillingService subscriptionBillingService;
    private final PlanSaasService planSaasService;
    private final TenantCatalogBootstrapService tenantCatalogBootstrapService;
    private final ProfessionalEmailService professionalEmailService;
    private final InscriptionOtpService inscriptionOtpService;
    private final StoredFileService storedFileService;
    private final AppMailService appMailService;

    @Value("${app.demande.public-base-url:http://localhost:5173}")
    private String publicBaseUrl;

    @Transactional
    public ParoisseInscriptionResponse soumettre(
            ParoisseInscriptionRequest request,
            MultipartFile mandatCure,
            MultipartFile adminCni
    ) {
        doyenneRepository.findByPublicIdAndStatusDelFalse(request.doyennePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Doyenné introuvable"));

        // Le front ne décide jamais seul du catalogue commercial : un code de
        // plan désactivé ou falsifié est rejeté côté serveur.
        PlanSaas selectedPlan = planSaasService.requireActive(request.planAbonnement());

        var proof = inscriptionOtpService.requireValidProof(
                request.adminEmail(),
                request.otpProof(),
                request.adminUsername(),
                request.adminPrenom(),
                request.adminNom(),
                request.nomParoisse()
        );

        if (userRepository.existsByUsernameIgnoreCaseAndStatusDelFalse(proof.username())
                || inscriptionRepository.existsByAdminUsernameIgnoreCaseAndStatusDelFalse(proof.username())) {
            throw new AlreadyExistException("Ce nom d'utilisateur est déjà pris");
        }

        // La paroisse figure presque toujours déjà dans l'annuaire diocésain :
        // son inscription la revendique. Seule une paroisse déjà engagée
        // commercialement bloque un nouveau dossier.
        paroisseRepository.findByNomIgnoreCaseAndDoyenne_PublicIdAndStatusDelFalse(
                        request.nomParoisse().trim(), request.doyennePublicId())
                .filter(existante -> existante.getStatutTenant() != StatutTenant.PROSPECT)
                .ifPresent(existante -> {
                    throw new AlreadyExistException(
                            "Cette paroisse est déjà rattachée à un compte sur la plateforme"
                    );
                });

        if (inscriptionRepository.existsByNomParoisseIgnoreCaseAndDoyennePublicIdAndStatutAndStatusDelFalse(
                request.nomParoisse().trim(), request.doyennePublicId(), StatutInscription.SOUMISE)) {
            throw new AlreadyExistException("Un dossier est déjà en cours d'examen pour cette paroisse");
        }

        String mandatPath = storedFileService.storeInscriptionDocument(mandatCure, "mandat");
        String cniPath = storedFileService.storeInscriptionDocument(adminCni, "cni");

        try {
            ParoisseInscription inscription = new ParoisseInscription();
            inscription.setNomParoisse(request.nomParoisse().trim());
            inscription.setAdresse(request.adresse().trim());
            inscription.setEmail(request.email());
            inscription.setTelephone(request.telephone());
            inscription.setDoyennePublicId(request.doyennePublicId());
            inscription.setPlanAbonnement(selectedPlan.getCode());
            inscription.setAdminNom(request.adminNom().trim());
            inscription.setAdminPrenom(request.adminPrenom().trim());
            inscription.setAdminEmail(proof.email());
            inscription.setAdminTelephone(request.adminTelephone());
            inscription.setAdminUsername(proof.username());
            // Mot de passe uniquement depuis la preuve OTP serveur (jamais du JSON client).
            inscription.setAdminPasswordHash(passwordEncoder.encode(proof.password()));
            inscription.setMessage(request.message());
            inscription.setMandatCurePath(mandatPath);
            inscription.setAdminCniPath(cniPath);
            inscription.setStatut(StatutInscription.SOUMISE);
            inscription.setStatusDel(false);

            if (request.membres() != null) {
                for (ParoisseInscriptionRequest.MembreRequest m : request.membres()) {
                    ParoisseInscriptionMembre membre = new ParoisseInscriptionMembre();
                    membre.setInscription(inscription);
                    membre.setNom(m.nom().trim());
                    membre.setPrenom(m.prenom().trim());
                    membre.setEmail(m.email());
                    membre.setTelephone(m.telephone());
                    membre.setRoleParoisse(m.roleParoisse());
                    membre.setUsername(m.username());
                    inscription.getMembres().add(membre);
                }
            }

            ParoisseInscriptionResponse response = toResponse(inscriptionRepository.save(inscription));
            return response;
        } catch (RuntimeException ex) {
            storedFileService.deleteQuietly(mandatPath);
            storedFileService.deleteQuietly(cniPath);
            throw ex;
        }
    }

    /**
     * Les doyennés, les paroisses et le petit catalogue SaaS sont préchargés
     * afin d'éviter un N+1 lors de l'affichage des dossiers.
     */
    @Transactional(readOnly = true)
    public List<ParoisseInscriptionResponse> listAll() {
        Map<UUID, String> doyennes = doyenneRepository.findAllByStatusDelFalseOrderByRangAscNomAsc()
                .stream()
                .collect(Collectors.toMap(Doyenne::getPublicId, Doyenne::getNom, (a, b) -> a));
        Map<UUID, Paroisse> paroisses = paroisseRepository.findAllByStatusDelFalseAndIsSystemFalseOrderByNomAsc()
                .stream()
                .collect(Collectors.toMap(Paroisse::getPublicId, p -> p, (a, b) -> a));
        Map<String, Integer> planAmounts = planSaasService.findAll()
                .stream()
                .collect(Collectors.toMap(PlanSaasResponse::code, PlanSaasResponse::montantXof, (a, b) -> a));

        return inscriptionRepository.findByStatusDelFalseOrderByCreatedAtDesc()
                .stream()
                .map(inscription -> toResponse(inscription, doyennes, paroisses, planAmounts))
                .toList();
    }

    @Transactional
    public Map<String, Object> approuver(UUID publicId) {
        ParoisseInscription inscription = inscriptionRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable"));
        if (inscription.getStatut() != StatutInscription.SOUMISE) {
            throw new BusinessRuleException("Cette inscription n'est plus en attente");
        }
        if (!StringUtils.hasText(inscription.getMandatCurePath())
                || !StringUtils.hasText(inscription.getAdminCniPath())) {
            throw new BusinessRuleException(
                    "Dossier incomplet : mandat du curé et pièce d'identité de l'administrateur requis"
            );
        }

        Doyenne doyenne = doyenneRepository.findByPublicIdAndStatusDelFalse(inscription.getDoyennePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Doyenné introuvable"));

        // Adopter l'entrée d'annuaire plutôt que d'en créer une seconde : sans
        // cela l'approbation heurterait l'index unique nom + doyenné, et les
        // liens publics déjà diffusés pointeraient vers la mauvaise paroisse.
        Paroisse paroisse = paroisseRepository
                .findByNomIgnoreCaseAndDoyenne_PublicIdAndStatusDelFalse(
                        inscription.getNomParoisse(), doyenne.getPublicId())
                .orElse(null);
        if (paroisse == null) {
            paroisse = new Paroisse();
        } else if (paroisse.getStatutTenant() != StatutTenant.PROSPECT) {
            throw new BusinessRuleException(
                    "Cette paroisse est déjà rattachée à un compte sur la plateforme"
            );
        }

        paroisse.setNom(inscription.getNomParoisse());
        paroisse.setAdresse(inscription.getAdresse());
        paroisse.setTelephone(inscription.getTelephone());
        paroisse.setDoyenne(doyenne);
        paroisse.appliquerStatut(StatutTenant.EN_ATTENTE_PAIEMENT);
        paroisse.setStatusDel(false);
        professionalEmailService.assignToParoisse(paroisse);
        paroisse = paroisseRepository.save(paroisse);
        parishLedgerService.ensureCompte(paroisse);
        tenantCatalogBootstrapService.seedDefaultsIfEmpty(paroisse);

        User admin = new User();
        admin.setNom(inscription.getAdminNom());
        admin.setPrenom(inscription.getAdminPrenom());
        admin.setUsername(inscription.getAdminUsername());
        admin.setTelephone(inscription.getAdminTelephone());
        admin.setPassword(inscription.getAdminPasswordHash());
        admin.setRole(UserRole.ADMIN);
        admin.setIsGlobal(false);
        admin.setIsActive(true);
        admin.setStatusDel(false);
        professionalEmailService.assignToUser(admin, paroisse);
        admin = userRepository.save(admin);

        ParoisseAccess access = new ParoisseAccess();
        access.setUser(admin);
        access.setParoisse(paroisse);
        access.setRoleParoisse(RoleParoisse.ADMIN);
        access.setActive(true);
        access.setStatusDel(false);
        paroisseAccessRepository.save(access);

        // Membres : comptes créés sans mot de passe définitif (username optionnel) — à compléter plus tard.
        for (ParoisseInscriptionMembre membre : inscription.getMembres()) {
            if (!StringUtils.hasText(membre.getUsername())) {
                continue;
            }
            if (userRepository.existsByUsernameIgnoreCaseAndStatusDelFalse(membre.getUsername())) {
                continue;
            }
            User memberUser = new User();
            memberUser.setNom(membre.getNom());
            memberUser.setPrenom(membre.getPrenom());
            memberUser.setUsername(membre.getUsername());
            memberUser.setTelephone(membre.getTelephone());
            memberUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            memberUser.setRole(UserRole.SECRETAIRE);
            memberUser.setIsGlobal(false);
            memberUser.setIsActive(false);
            memberUser.setStatusDel(false);
            professionalEmailService.assignToUser(memberUser, paroisse);
            memberUser = userRepository.save(memberUser);

            ParoisseAccess memberAccess = new ParoisseAccess();
            memberAccess.setUser(memberUser);
            memberAccess.setParoisse(paroisse);
            memberAccess.setRoleParoisse(membre.getRoleParoisse());
            memberAccess.setActive(true);
            memberAccess.setStatusDel(false);
            paroisseAccessRepository.save(memberAccess);
        }

        inscription.setStatut(StatutInscription.APPROUVEE);
        inscription.setParoissePublicId(paroisse.getPublicId());
        // PII : purger scans + hash mdp après création du compte User (déjà hashé dessus).
        String mandatPath = inscription.getMandatCurePath();
        String cniPath = inscription.getAdminCniPath();
        inscription.setMandatCurePath(null);
        inscription.setAdminCniPath(null);
        inscription.setAdminPasswordHash(null);
        inscriptionRepository.save(inscription);
        storedFileService.deleteQuietly(mandatPath);
        storedFileService.deleteQuietly(cniPath);

        Map<String, Object> checkout = subscriptionBillingService.checkout(
                paroisse.getPublicId(),
                inscription.getPlanAbonnement()
        );

        return Map.of(
                "inscription", toResponse(inscription),
                "paroissePublicId", paroisse.getPublicId(),
                "abonnementCheckout", checkout,
                "emailParoisse", paroisse.getEmail(),
                "emailAdmin", admin.getEmail()
        );
    }

    @Transactional
    public ParoisseInscriptionResponse rejeter(UUID publicId, String motif) {
        ParoisseInscription inscription = inscriptionRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable"));
        if (inscription.getStatut() != StatutInscription.SOUMISE) {
            throw new BusinessRuleException("Cette inscription n'est plus en attente");
        }
        String cleanedMotif = motif == null ? "" : motif.trim();
        if (!StringUtils.hasText(cleanedMotif) || cleanedMotif.length() < 8) {
            throw new BusinessRuleException(
                    "Indiquez un motif de rejet (au moins 8 caractères) pour informer la paroisse."
            );
        }
        inscription.setStatut(StatutInscription.REJETEE);
        inscription.setMessage(cleanedMotif);
        // PII : purger scans + hash mdp après rejet.
        String mandatPath = inscription.getMandatCurePath();
        String cniPath = inscription.getAdminCniPath();
        inscription.setMandatCurePath(null);
        inscription.setAdminCniPath(null);
        inscription.setAdminPasswordHash(null);
        ParoisseInscription saved = inscriptionRepository.save(inscription);
        storedFileService.deleteQuietly(mandatPath);
        storedFileService.deleteQuietly(cniPath);
        notifyRejection(saved, cleanedMotif);
        return toResponse(saved);
    }

    private void notifyRejection(ParoisseInscription inscription, String motif) {
        String to = StringUtils.hasText(inscription.getAdminEmail())
                ? inscription.getAdminEmail().trim()
                : (StringUtils.hasText(inscription.getEmail()) ? inscription.getEmail().trim() : null);
        if (!StringUtils.hasText(to)) {
            return;
        }
        String subject = "Inscription refusée — " + inscription.getNomParoisse();
        String body = """
                Bonjour,

                Votre demande d'inscription pour « %s » a été refusée.

                Motif : %s

                Vous pouvez déposer un nouveau dossier corrigé sur %s/inscription-paroisse

                — Missanye
                """.formatted(
                inscription.getNomParoisse(),
                motif,
                publicBaseUrl.replaceAll("/+$", "")
        );
        appMailService.sendTextAsync(to, subject, body);
    }

    @Transactional(readOnly = true)
    public Resource loadDocument(UUID publicId, String documentType) {
        ParoisseInscription inscription = inscriptionRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable"));
        String path = switch (documentType == null ? "" : documentType.trim().toLowerCase()) {
            case "mandat", "mandat-cure", "mandat_cure" -> inscription.getMandatCurePath();
            case "cni", "admin-cni", "admin_cni" -> inscription.getAdminCniPath();
            default -> throw new BusinessRuleException("Type de document inconnu (mandat | cni)");
        };
        if (!StringUtils.hasText(path)) {
            throw new ResourceNotFoundException("Document non déposé sur ce dossier");
        }
        return storedFileService.loadAsResource(path);
    }

    @Transactional(readOnly = true)
    public String documentContentType(UUID publicId, String documentType) {
        ParoisseInscription inscription = inscriptionRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable"));
        String path = switch (documentType == null ? "" : documentType.trim().toLowerCase()) {
            case "mandat", "mandat-cure", "mandat_cure" -> inscription.getMandatCurePath();
            case "cni", "admin-cni", "admin_cni" -> inscription.getAdminCniPath();
            default -> throw new BusinessRuleException("Type de document inconnu (mandat | cni)");
        };
        return storedFileService.detectContentType(path);
    }

    private ParoisseInscriptionResponse toResponse(ParoisseInscription inscription) {
        int montant = planSaasService.require(inscription.getPlanAbonnement()).getMontantXof();
        return toResponse(
                inscription,
                Map.of(),
                Map.of(),
                Map.of(inscription.getPlanAbonnement(), montant)
        );
    }

    private ParoisseInscriptionResponse toResponse(
            ParoisseInscription inscription,
            Map<UUID, String> doyennes,
            Map<UUID, Paroisse> paroisses,
            Map<String, Integer> planAmounts
    ) {
        List<ParoisseInscriptionResponse.MembreResponse> membres = new ArrayList<>();
        if (inscription.getMembres() != null) {
            for (ParoisseInscriptionMembre m : inscription.getMembres()) {
                membres.add(new ParoisseInscriptionResponse.MembreResponse(
                        m.getNom(), m.getPrenom(), m.getEmail(), m.getTelephone(),
                        m.getRoleParoisse(), m.getUsername()
                ));
            }
        }

        String doyenneNom = doyennes.get(inscription.getDoyennePublicId());
        if (doyenneNom == null && inscription.getDoyennePublicId() != null) {
            doyenneNom = doyenneRepository
                    .findByPublicIdAndStatusDelFalse(inscription.getDoyennePublicId())
                    .map(Doyenne::getNom)
                    .orElse(null);
        }

        Paroisse paroisse = null;
        if (inscription.getParoissePublicId() != null) {
            paroisse = paroisses.get(inscription.getParoissePublicId());
            if (paroisse == null) {
                paroisse = paroisseRepository
                        .findByPublicIdAndStatusDelFalse(inscription.getParoissePublicId())
                        .orElse(null);
            }
        }

        Integer montantAbonnement = planAmounts.get(inscription.getPlanAbonnement());
        if (montantAbonnement == null) {
            montantAbonnement = planSaasService.require(inscription.getPlanAbonnement()).getMontantXof();
        }

        return new ParoisseInscriptionResponse(
                inscription.getPublicId(),
                inscription.getNomParoisse(),
                inscription.getAdresse(),
                inscription.getEmail(),
                inscription.getTelephone(),
                inscription.getDoyennePublicId(),
                doyenneNom,
                inscription.getPlanAbonnement(),
                montantAbonnement,
                inscription.getAdminNom(),
                inscription.getAdminPrenom(),
                inscription.getAdminEmail(),
                inscription.getAdminTelephone(),
                inscription.getAdminUsername(),
                inscription.getStatut(),
                inscription.getMessage(),
                inscription.getParoissePublicId(),
                paroisse != null ? paroisse.getIsActive() : null,
                paroisse != null ? paroisse.getSubscriptionExpiresAt() : null,
                inscription.getCreatedAt(),
                StringUtils.hasText(inscription.getMandatCurePath()),
                StringUtils.hasText(inscription.getAdminCniPath()),
                membres
        );
    }
}
