package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.HoraireRequest;
import com.eyram.dev.church_project_spring.DTO.response.HoraireResponse;
import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.mappers.HoraireMapper;
import com.eyram.dev.church_project_spring.repositories.HoraireRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.service.HoraireService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HoraireServiceImpl implements HoraireService {

    private final HoraireRepository horaireRepository;
    private final ParoisseRepository paroisseRepository;
    private final HoraireMapper horaireMapper;

    @Override
    public HoraireResponse create(HoraireRequest request) {

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

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

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

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
    public HoraireResponse getByPublicId(UUID publicId) {
        Horaire horaire = horaireRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable"));

        return horaireMapper.modelToDto(horaire);
    }

    @Override
    public List<HoraireResponse> getAll() {
        return horaireRepository.findByStatusDelFalse()
                .stream()
                .map(horaireMapper::modelToDto)
                .toList();
    }

    @Override
    public List<HoraireResponse> getByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        return horaireRepository.findByParoisseAndStatusDelFalse(paroisse)
                .stream()
                .map(horaireMapper::modelToDto)
                .toList();
    }

    @Override
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

        horaire.setStatusDel(true);
        horaireRepository.save(horaire);
    }
}