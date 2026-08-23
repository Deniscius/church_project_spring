package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.DemandeDateRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeDateResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.mappers.DemandeDateMapper;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.service.DemandeSchedulingPolicy;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.DemandeDateService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DemandeDateServiceImpl implements DemandeDateService {

    private final DemandeDateRepository demandeDateRepository;
    private final DemandeRepository demandeRepository;
    private final DemandeDateMapper demandeDateMapper;
    private final TenantAccessService tenantAccessService;
    private final DemandeSchedulingPolicy schedulingPolicy;

    @Override
    public DemandeDateResponse create(DemandeDateRequest request) {

        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(request.demandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
        tenantAccessService.checkParoisseAccess(demande.getParoisse());
        validateSchedule(demande, request.dateCelebration(), resolveTime(demande));

        if (demandeDateRepository.existsByDemandeAndOrdreAndStatusDelFalse(demande, request.ordre())) {
            throw new AlreadyExistException("Cet ordre existe déjà pour cette demande");
        }

        if (demandeDateRepository.existsByDemandeAndDateCelebrationAndStatusDelFalse(demande, request.dateCelebration())) {
            throw new AlreadyExistException("Cette date existe déjà pour cette demande");
        }

        DemandeDate demandeDate = demandeDateMapper.dtoToModel(request);
        demandeDate.setDemande(demande);
        demandeDate.setHoraire(demande.getHoraire());
        demandeDate.setHeurePersonnalisee(demande.getHeurePersonnalisee());

        DemandeDate savedDemandeDate = demandeDateRepository.save(demandeDate);
        return demandeDateMapper.modelToDto(savedDemandeDate);
    }

    @Override
    public DemandeDateResponse update(UUID publicId, DemandeDateRequest request) {

        DemandeDate existingDemandeDate = demandeDateRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Date de demande introuvable"));
        tenantAccessService.checkParoisseAccess(existingDemandeDate.getDemande().getParoisse());

        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(request.demandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
        tenantAccessService.checkParoisseAccess(demande.getParoisse());
        validateSchedule(demande, request.dateCelebration(), resolveTime(existingDemandeDate));

        boolean ordreChanged =
                !existingDemandeDate.getOrdre().equals(request.ordre()) ||
                        !existingDemandeDate.getDemande().getPublicId().equals(request.demandePublicId());

        if (ordreChanged && demandeDateRepository.existsByDemandeAndOrdreAndStatusDelFalse(demande, request.ordre())) {
            throw new AlreadyExistException("Cet ordre existe déjà pour cette demande");
        }

        boolean dateChanged =
                !existingDemandeDate.getDateCelebration().equals(request.dateCelebration()) ||
                        !existingDemandeDate.getDemande().getPublicId().equals(request.demandePublicId());

        if (dateChanged && demandeDateRepository.existsByDemandeAndDateCelebrationAndStatusDelFalse(demande, request.dateCelebration())) {
            throw new AlreadyExistException("Cette date existe déjà pour cette demande");
        }

        demandeDateMapper.updateEntityFromDto(request, existingDemandeDate);
        existingDemandeDate.setDemande(demande);

        DemandeDate updatedDemandeDate = demandeDateRepository.save(existingDemandeDate);
        return demandeDateMapper.modelToDto(updatedDemandeDate);
    }

    @Override
    public DemandeDateResponse getByPublicId(UUID publicId) {
        DemandeDate demandeDate = demandeDateRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Date de demande introuvable"));
        tenantAccessService.checkParoisseAccess(demandeDate.getDemande().getParoisse());

        return demandeDateMapper.modelToDto(demandeDate);
    }

    @Override
    public List<DemandeDateResponse> getAll() {
        return demandeDateRepository.findByStatusDelFalse()
                .stream()
                .filter(demandeDate -> tenantAccessService.isGlobalUser()
                        || tenantAccessService.canAccessParoisse(demandeDate.getDemande().getParoisse()))
                .map(demandeDateMapper::modelToDto)
                .toList();
    }

    @Override
    public List<DemandeDateResponse> getByDemande(UUID demandePublicId) {
        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(demandePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
        tenantAccessService.checkParoisseAccess(demande.getParoisse());

        return demandeDateRepository.findByDemandeAndStatusDelFalseOrderByOrdreAsc(demande)
                .stream()
                .map(demandeDateMapper::modelToDto)
                .toList();
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        DemandeDate demandeDate = demandeDateRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Date de demande introuvable"));
        tenantAccessService.checkParoisseAccess(demandeDate.getDemande().getParoisse());

        if (Boolean.TRUE.equals(demandeDate.getCelebre())) {
            throw new BusinessRuleException("Une célébration déjà confirmée ne peut pas être supprimée");
        }
        if (demandeDate.getDemande().getStatutPaiement()
                == com.eyram.dev.church_project_spring.enums.StatutPaiementEnum.PAYE) {
            throw new BusinessRuleException(
                    "Une date déjà payée ne peut pas être supprimée sans procédure de remboursement"
            );
        }

        demandeDate.setStatusDel(true);
        demandeDateRepository.save(demandeDate);
    }
    private void validateSchedule(Demande demande, java.time.LocalDate date, LocalTime time) {
        Integer leadHours = demande.getTypeDemande() != null
                ? demande.getTypeDemande().getDelaiMinimumHeures()
                : null;
        schedulingPolicy.validate(date, time, leadHours);
    }

    private LocalTime resolveTime(Demande demande) {
        if (demande.getHeurePersonnalisee() != null) {
            return demande.getHeurePersonnalisee();
        }
        return demande.getHoraire() != null ? demande.getHoraire().getHeureCelebration() : null;
    }

    private LocalTime resolveTime(DemandeDate demandeDate) {
        if (demandeDate.getHeurePersonnalisee() != null) {
            return demandeDate.getHeurePersonnalisee();
        }
        if (demandeDate.getHoraire() != null) {
            return demandeDate.getHoraire().getHeureCelebration();
        }
        return resolveTime(demandeDate.getDemande());
    }

}
