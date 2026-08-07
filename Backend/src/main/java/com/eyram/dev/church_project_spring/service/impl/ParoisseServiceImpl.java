package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseCoordonneesRequest;
import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.AnnuaireParoisseResponse;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.entities.Doyenne;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutTenant;
import com.eyram.dev.church_project_spring.mappers.ParoisseMapper;
import com.eyram.dev.church_project_spring.repositories.DoyenneRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.ParoisseService;
import com.eyram.dev.church_project_spring.service.ProfessionalEmailService;
import com.eyram.dev.church_project_spring.service.storage.StoredFileService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ParoisseServiceImpl implements ParoisseService {

    private final ParoisseRepository paroisseRepository;
    private final ParoisseAccessRepository paroisseAccessRepository;
    private final DoyenneRepository doyenneRepository;
    private final ParoisseMapper paroisseMapper;
    private final TenantAccessService tenantAccessService;
    private final ProfessionalEmailService professionalEmailService;
    private final StoredFileService storedFileService;

    @Override
    public ParoisseResponse create(ParoisseRequest request) {
        ParoisseRequest normalizedRequest = normalize(request);

        if (!tenantAccessService.isGlobalUser()) {
            throw new AccessDeniedException("Seul un utilisateur global peut créer une paroisse");
        }

        Doyenne doyenne = findActiveDoyenne(normalizedRequest.doyennePublicId());

        boolean exists = paroisseRepository.existsByNomIgnoreCaseAndDoyenne_PublicIdAndStatusDelFalse(
                normalizedRequest.nom(),
                normalizedRequest.doyennePublicId()
        );

        if (exists) {
            throw new AlreadyExistException("Cette paroisse existe déjà dans ce doyenné");
        }

        Paroisse paroisse = paroisseMapper.dtoToModel(normalizedRequest);
        paroisse.setPublicId(UUID.randomUUID());
        paroisse.setStatusDel(false);
        // Entrée d'annuaire : la paroisse existe dans le doyenné mais n'est pas
        // encore cliente. L'inscription publique (ou l'activation comptable) la
        // fera passer en EN_ATTENTE_PAIEMENT puis ACTIVE — sans créer de tenant
        // gratuit, et sans cloner un catalogue pour une fiche encore inactive.
        paroisse.appliquerStatut(StatutTenant.PROSPECT);
        paroisse.setDoyenne(doyenne);
        professionalEmailService.assignToParoisse(paroisse);

        return paroisseMapper.modelToDto(paroisseRepository.save(paroisse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParoisseResponse> getAll() {
        // Catalogue public : paroisses actives uniquement.
        // SUPER_ADMIN global : toutes les paroisses (y compris en attente d'abonnement).
        List<Paroisse> paroisses = tenantAccessService.isGlobalUser()
                ? paroisseRepository.findAllByStatusDelFalseAndIsSystemFalseOrderByNomAsc()
                : paroisseRepository.findAllByStatusDelFalseAndIsActiveTrueAndIsSystemFalseOrderByNomAsc();
        return paroisses.stream().map(paroisseMapper::modelToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnuaireParoisseResponse> getAnnuaireDisponible(UUID doyennePublicId) {
        if (doyennePublicId == null) {
            throw new IllegalArgumentException("Le doyenné est obligatoire");
        }
        return paroisseRepository
                .findAllByStatutTenantAndDoyenne_PublicIdAndStatusDelFalseAndIsSystemFalseOrderByNomAsc(
                        StatutTenant.PROSPECT, doyennePublicId)
                .stream()
                .map(p -> new AnnuaireParoisseResponse(p.getPublicId(), p.getNom(), p.getAdresse()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ParoisseResponse getByPublicId(UUID publicId) {
        Paroisse paroisse = findActiveParoisse(publicId);

        return paroisseMapper.modelToDto(paroisse);
    }

    @Override
    public ParoisseResponse update(UUID publicId, ParoisseRequest request) {
        ParoisseRequest normalizedRequest = normalize(request);
        Paroisse paroisse = findActiveParoisse(publicId);

        tenantAccessService.checkParoisseAccess(paroisse);

        Doyenne doyenne = findActiveDoyenne(normalizedRequest.doyennePublicId());

        boolean exists = paroisseRepository
                .existsByNomIgnoreCaseAndDoyenne_PublicIdAndStatusDelFalseAndPublicIdNot(
                        normalizedRequest.nom(),
                        normalizedRequest.doyennePublicId(),
                        publicId
        );

        if (exists) {
            throw new AlreadyExistException("Une autre paroisse avec ce nom existe déjà dans ce doyenné");
        }

        paroisseMapper.updateEntityFromDto(normalizedRequest, paroisse);
        paroisse.setDoyenne(doyenne);
        // E-mail pro immuable une fois attribué ; sinon (re)génération automatique.
        if (!professionalEmailService.isProfessional(paroisse.getEmail())) {
            professionalEmailService.assignToParoisse(paroisse);
        }

        Paroisse updatedParoisse = paroisseRepository.save(paroisse);
        return paroisseMapper.modelToDto(updatedParoisse);
    }

    @Override
    public ParoisseResponse updateCoordonnees(UUID publicId, ParoisseCoordonneesRequest request) {
        Paroisse paroisse = findActiveParoisse(publicId);
        tenantAccessService.checkParoisseAccess(paroisse);

        // L'e-mail reste une adresse professionnelle générée ; le téléphone et le RIB sont libres.
        if (!professionalEmailService.isProfessional(paroisse.getEmail())) {
            professionalEmailService.assignToParoisse(paroisse);
        }
        paroisse.setTelephone(blankToNull(request.telephone()));
        paroisse.setNomBanque(blankToNull(request.nomBanque()));
        paroisse.setTitulaireCompte(blankToNull(request.titulaireCompte()));
        paroisse.setIbanOrRib(blankToNull(request.ibanOrRib()));

        return paroisseMapper.modelToDto(paroisseRepository.save(paroisse));
    }

    @Override
    public ParoisseResponse updateLogo(UUID publicId, MultipartFile logo) {
        Paroisse paroisse = findActiveParoisse(publicId);
        tenantAccessService.checkParoisseAccess(paroisse);
        requireReceiptCustomizationAllowed(paroisse);

        String previous = paroisse.getLogoPath();
        String stored = storedFileService.storeParishLogo(logo, paroisse.getPublicId());
        paroisse.setLogoPath(stored);
        ParoisseResponse response = paroisseMapper.modelToDto(paroisseRepository.save(paroisse));
        if (StringUtils.hasText(previous) && !previous.equals(stored)) {
            storedFileService.deleteQuietly(previous);
        }
        return response;
    }

    @Override
    public ParoisseResponse removeLogo(UUID publicId) {
        Paroisse paroisse = findActiveParoisse(publicId);
        tenantAccessService.checkParoisseAccess(paroisse);
        requireReceiptCustomizationAllowed(paroisse);

        String previous = paroisse.getLogoPath();
        paroisse.setLogoPath(null);
        ParoisseResponse response = paroisseMapper.modelToDto(paroisseRepository.save(paroisse));
        storedFileService.deleteQuietly(previous);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadLogo(UUID publicId) {
        Paroisse paroisse = findActiveParoisse(publicId);
        if (!StringUtils.hasText(paroisse.getLogoPath())) {
            throw new ResourceNotFoundException("Aucun logo configuré pour cette paroisse");
        }
        return storedFileService.loadAsResource(paroisse.getLogoPath());
    }

    @Override
    @Transactional(readOnly = true)
    public String logoContentType(UUID publicId) {
        Paroisse paroisse = findActiveParoisse(publicId);
        if (!StringUtils.hasText(paroisse.getLogoPath())) {
            throw new ResourceNotFoundException("Aucun logo configuré pour cette paroisse");
        }
        return storedFileService.detectContentType(paroisse.getLogoPath());
    }

    /**
     * Personnalisation du reçu (logo) uniquement après validation / activation
     * du compte paroisse sur la plateforme.
     */
    private void requireReceiptCustomizationAllowed(Paroisse paroisse) {
        StatutTenant statut = paroisse.getStatutTenant();
        if (statut != StatutTenant.ACTIVE && statut != StatutTenant.EN_TOLERANCE) {
            throw new BusinessRuleException(
                    "La personnalisation du reçu est disponible après validation et activation du compte"
            );
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        Paroisse paroisse = findActiveParoisse(publicId);

        tenantAccessService.checkParoisseAccess(paroisse);

        paroisseAccessRepository.findByParoisseAndStatusDelFalse(paroisse)
                .forEach(access -> {
                    access.setActive(false);
                    access.setStatusDel(true);
                });
        paroisse.appliquerStatut(StatutTenant.RESILIEE);
        paroisse.setStatusDel(true);
        paroisseRepository.save(paroisse);
    }

    private Paroisse findActiveParoisse(UUID publicId) {
        if (publicId == null) {
            throw new IllegalArgumentException("L'identifiant de la paroisse est obligatoire");
        }
        return paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse non trouvée"));
    }

    private Doyenne findActiveDoyenne(UUID publicId) {
        if (publicId == null) {
            throw new IllegalArgumentException("Le doyenné est obligatoire");
        }
        return doyenneRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Doyenné non trouvé"));
    }

    private ParoisseRequest normalize(ParoisseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête paroisse est obligatoire");
        }

        return new ParoisseRequest(
                normalizeRequired(request.nom(), 2, 100,
                        "Le nom est obligatoire",
                        "Le nom doit contenir entre 2 et 100 caractères"),
                normalizeRequired(request.adresse(), 3, 200,
                        "L'adresse est obligatoire",
                        "L'adresse doit contenir entre 3 et 200 caractères"),
                normalizeEmail(request.email()),
                normalizeOptional(request.telephone(), 3, 50,
                        "Le téléphone doit contenir entre 3 et 50 caractères"),
                normalizeOptional(request.nomBanque(), 2, 120,
                        "Le nom de banque doit contenir entre 2 et 120 caractères"),
                normalizeOptional(request.titulaireCompte(), 2, 150,
                        "Le titulaire doit contenir entre 2 et 150 caractères"),
                normalizeOptional(request.ibanOrRib(), 5, 80,
                        "Le RIB/IBAN doit contenir entre 5 et 80 caractères"),
                request.doyennePublicId()
        );
    }

    private String normalizeRequired(
            String value,
            int minLength,
            int maxLength,
            String requiredMessage,
            String lengthMessage
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(requiredMessage);
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        validateLength(normalized, minLength, maxLength, lengthMessage);
        return normalized;
    }

    private String normalizeOptional(String value, int minLength, int maxLength, String lengthMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        validateLength(normalized, minLength, maxLength, lengthMessage);
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeOptional(
                email,
                3,
                150,
                "L'email doit contenir entre 3 et 150 caractères"
        );
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private void validateLength(String value, int minLength, int maxLength, String message) {
        if (value.length() < minLength || value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }
}
