package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.HoraireRequest;
import com.eyram.dev.church_project_spring.DTO.response.HoraireResponse;
import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.mappers.HoraireMapper;
import com.eyram.dev.church_project_spring.repositories.HoraireRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.HoraireService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HoraireServiceImpl implements HoraireService {

    private final HoraireRepository horaireRepository;
    private final ParoisseRepository paroisseRepository;
    private final HoraireMapper horaireMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    public HoraireResponse create(HoraireRequest request) {

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        tenantAccessService.checkParoisseAccess(paroisse);

        boolean exists = horaireRepository.existsByJourSemaineAndHeureCelebrationAndParoisseAndStatusDelFalse(
                request.jourSemaine(),
                request.heureCelebration(),
                paroisse
        );

        if (exists) {
            throw new AlreadyExistException("Cet horaire existe déjà pour cette paroisse");
        }

        Horaire horaire = horaireMapper.dtoToModel(request);
        horaire.setParoisse(paroisse);

        Horaire savedHoraire = horaireRepository.save(horaire);
        return horaireMapper.modelToDto(savedHoraire);
    }

    @Override
    public HoraireResponse update(UUID publicId, HoraireRequest request) {

        Horaire existingHoraire = horaireRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable"));

        tenantAccessService.checkParoisseAccess(existingHoraire.getParoisse());

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        tenantAccessService.checkParoisseAccess(paroisse);

        boolean dataChanged =
                !existingHoraire.getJourSemaine().equals(request.jourSemaine()) ||
                !existingHoraire.getHeureCelebration().equals(request.heureCelebration()) ||
                !existingHoraire.getParoisse().getPublicId().equals(request.paroissePublicId());

        if (dataChanged) {
            boolean exists = horaireRepository.existsByJourSemaineAndHeureCelebrationAndParoisseAndStatusDelFalse(
                    request.jourSemaine(),
                    request.heureCelebration(),
                    paroisse
            );

            if (exists) {
                throw new AlreadyExistException("Cet horaire existe déjà pour cette paroisse");
            }
        }

        horaireMapper.updateEntityFromDto(request, existingHoraire);
        existingHoraire.setParoisse(paroisse);

        Horaire updatedHoraire = horaireRepository.save(existingHoraire);
        return horaireMapper.modelToDto(updatedHoraire);
    }

    @Override
    @Transactional(readOnly = true)
    public HoraireResponse getByPublicId(UUID publicId) {
        Horaire horaire = horaireRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable"));

        tenantAccessService.checkParoisseAccess(horaire.getParoisse());

        return horaireMapper.modelToDto(horaire);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoraireResponse> getAll() {
        if (tenantAccessService.isGlobalUser()) {
            return horaireRepository.findByStatusDelFalse()
                    .stream()
                    .map(horaireMapper::modelToDto)
                    .toList();
        }

        return horaireRepository.findByStatusDelFalse()
                .stream()
                .filter(horaire -> tenantAccessService.canAccessParoisse(horaire.getParoisse()))
                .map(horaireMapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoraireResponse> getByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        return horaireRepository.findByParoisseAndStatusDelFalse(paroisse)
                .stream()
                .map(horaireMapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoraireResponse> getActiveByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        return horaireRepository.findByParoisseAndIsActiveTrueAndStatusDelFalse(paroisse)
                .stream()
                .map(horaireMapper::modelToDto)
                .toList();
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        Horaire horaire = horaireRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable"));

        tenantAccessService.checkParoisseAccess(horaire.getParoisse());

        horaire.setIsActive(false);
        horaire.setStatusDel(true);
        horaireRepository.save(horaire);
    }
}