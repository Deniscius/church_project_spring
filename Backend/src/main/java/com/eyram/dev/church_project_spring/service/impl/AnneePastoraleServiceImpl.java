package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.AnneePastoraleRequest;
import com.eyram.dev.church_project_spring.DTO.response.AnneePastoraleResponse;
import com.eyram.dev.church_project_spring.config.ApplicationTimeConfig;
import com.eyram.dev.church_project_spring.entities.AnneePastorale;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutAnneePastorale;
import com.eyram.dev.church_project_spring.mappers.AnneePastoraleMapper;
import com.eyram.dev.church_project_spring.repositories.AnneePastoraleRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.AnneePastoraleService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AnneePastoraleServiceImpl implements AnneePastoraleService {

    private final AnneePastoraleRepository anneePastoraleRepository;
    private final ParoisseRepository paroisseRepository;
    private final AnneePastoraleMapper anneePastoraleMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    public AnneePastoraleResponse create(AnneePastoraleRequest request) {
        // Le verrou paroisse sérialise les contrôles de chevauchement concurrents.
        Paroisse paroisse = requireParoisseForUpdate(request.paroissePublicId());
        tenantAccessService.checkParoisseAccess(paroisse);

        String libelle = normalizeRequired(request.libelle());
        validatePeriod(request.dateDebut(), request.dateFin());
        assertNoDuplicate(paroisse, libelle, null);
        assertNoOverlap(paroisse, request.dateDebut(), request.dateFin(), null);

        AnneePastorale entity = anneePastoraleMapper.dtoToModel(request);
        entity.setParoisse(paroisse);
        entity.setLibelle(libelle);
        entity.setDescription(normalizeNullable(request.description()));
        entity.setStatut(StatutAnneePastorale.BROUILLON);
        entity.setStatusDel(false);

        return anneePastoraleMapper.modelToDto(anneePastoraleRepository.saveAndFlush(entity));
    }

    @Override
    public AnneePastoraleResponse update(UUID publicId, AnneePastoraleRequest request) {
        AnneePastorale entity = requireAnnee(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        assertVersion(entity, request.version());

        if (entity.getStatut() != StatutAnneePastorale.BROUILLON) {
            throw new BusinessRuleException("Seule une année pastorale en brouillon peut être modifiée");
        }
        if (!entity.getParoisse().getPublicId().equals(request.paroissePublicId())) {
            throw new BusinessRuleException("Une année pastorale ne peut pas être déplacée vers une autre paroisse");
        }
        requireParoisseForUpdate(entity.getParoisse().getPublicId());

        String libelle = normalizeRequired(request.libelle());
        validatePeriod(request.dateDebut(), request.dateFin());
        assertNoDuplicate(entity.getParoisse(), libelle, publicId);
        assertNoOverlap(entity.getParoisse(), request.dateDebut(), request.dateFin(), publicId);

        anneePastoraleMapper.updateEntityFromDto(request, entity);
        entity.setLibelle(libelle);
        entity.setDescription(normalizeNullable(request.description()));

        return anneePastoraleMapper.modelToDto(anneePastoraleRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public AnneePastoraleResponse getByPublicId(UUID publicId) {
        AnneePastorale entity = requireAnnee(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        return anneePastoraleMapper.modelToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnneePastoraleResponse> getByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = requireParoisse(paroissePublicId);
        tenantAccessService.checkParoisseAccess(paroisse);
        return anneePastoraleRepository.findByParoisseAndStatusDelFalseOrderByDateDebutDesc(paroisse)
                .stream()
                .map(anneePastoraleMapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnneePastoraleResponse getPublishedByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = requireParoisse(paroissePublicId);
        tenantAccessService.checkParoisseAccess(paroisse);
        AnneePastorale entity = anneePastoraleRepository
                .findByParoisseAndStatutAndStatusDelFalse(paroisse, StatutAnneePastorale.PUBLIEE)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune année pastorale publiée"));
        return anneePastoraleMapper.modelToDto(entity);
    }

    @Override
    public AnneePastoraleResponse publish(UUID publicId, Long version) {
        AnneePastorale entity = requireAnnee(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        assertVersion(entity, version);

        if (entity.getStatut() != StatutAnneePastorale.BROUILLON) {
            throw new BusinessRuleException("Seule une année pastorale en brouillon peut être publiée");
        }
        if (anneePastoraleRepository.findByParoisseAndStatutAndStatusDelFalse(
                entity.getParoisse(), StatutAnneePastorale.PUBLIEE).isPresent()) {
            throw new AlreadyExistException(
                    "Une année pastorale est déjà publiée pour cette paroisse ; clôturez-la d'abord"
            );
        }

        entity.setStatut(StatutAnneePastorale.PUBLIEE);
        entity.setPublishedAt(LocalDateTime.now(ApplicationTimeConfig.BUSINESS_ZONE));
        return anneePastoraleMapper.modelToDto(anneePastoraleRepository.saveAndFlush(entity));
    }

    @Override
    public AnneePastoraleResponse close(UUID publicId, Long version) {
        AnneePastorale entity = requireAnnee(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        assertVersion(entity, version);

        if (entity.getStatut() != StatutAnneePastorale.PUBLIEE) {
            throw new BusinessRuleException("Seule une année pastorale publiée peut être clôturée");
        }

        entity.setStatut(StatutAnneePastorale.CLOTUREE);
        entity.setClosedAt(LocalDateTime.now(ApplicationTimeConfig.BUSINESS_ZONE));
        return anneePastoraleMapper.modelToDto(anneePastoraleRepository.saveAndFlush(entity));
    }

    @Override
    public void deleteByPublicId(UUID publicId, Long version) {
        AnneePastorale entity = requireAnnee(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        assertVersion(entity, version);

        if (entity.getStatut() != StatutAnneePastorale.BROUILLON) {
            throw new BusinessRuleException(
                    "Une année pastorale publiée ou clôturée doit être conservée dans l'historique"
            );
        }
        entity.setStatusDel(true);
        anneePastoraleRepository.save(entity);
    }

    private AnneePastorale requireAnnee(UUID publicId) {
        return anneePastoraleRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Année pastorale introuvable"));
    }

    private Paroisse requireParoisse(UUID publicId) {
        return paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
    }

    private Paroisse requireParoisseForUpdate(UUID publicId) {
        return paroisseRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
    }

    private void assertNoDuplicate(Paroisse paroisse, String libelle, UUID excludedPublicId) {
        boolean exists = excludedPublicId == null
                ? anneePastoraleRepository.existsByParoisseAndLibelleIgnoreCaseAndStatusDelFalse(paroisse, libelle)
                : anneePastoraleRepository
                        .existsByParoisseAndLibelleIgnoreCaseAndStatusDelFalseAndPublicIdNot(
                                paroisse, libelle, excludedPublicId
                        );
        if (exists) {
            throw new AlreadyExistException("Une année pastorale avec ce libellé existe déjà");
        }
    }

    private void assertNoOverlap(
            Paroisse paroisse,
            LocalDate dateDebut,
            LocalDate dateFin,
            UUID excludedPublicId
    ) {
        if (anneePastoraleRepository.existsOverlappingPeriod(
                paroisse, dateDebut, dateFin, excludedPublicId)) {
            throw new BusinessRuleException(
                    "La période chevauche une autre année pastorale de cette paroisse"
            );
        }
    }

    private void validatePeriod(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null) {
            throw new BusinessRuleException("Les dates de début et de fin sont obligatoires");
        }
        if (!dateFin.isAfter(dateDebut)) {
            throw new BusinessRuleException("La date de fin doit être postérieure à la date de début");
        }
    }

    private void assertVersion(AnneePastorale entity, Long requestedVersion) {
        if (requestedVersion == null) {
            throw new BusinessRuleException("La version est obligatoire pour cette opération");
        }
        if (!Objects.equals(entity.getVersion(), requestedVersion)) {
            throw new BusinessRuleException(
                    "Cette année pastorale a été modifiée par un autre utilisateur ; rechargez les données"
            );
        }
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Le libellé est obligatoire");
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
