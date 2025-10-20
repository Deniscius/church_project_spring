package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.HoraireRequest;
import com.eyram.dev.church_project_spring.DTO.response.HoraireResponse;
import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.mappers.HoraireMapper;
import com.eyram.dev.church_project_spring.repositories.HoraireRepository;
import com.eyram.dev.church_project_spring.service.HoraireService;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import com.eyram.dev.church_project_spring.utils.exception.TrackingIdNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class HoraireServiceImpl implements HoraireService {

    private final HoraireRepository horaireRepository;
    private final HoraireMapper horaireMapper;

    @Override
    public HoraireResponse create(HoraireRequest request) {
        if (request == null) {
            throw new RequestNotFoundException("La requête d'horaire n'a pas été reçue");
        }
        validate(request);

        // éviter les doublons actifs (même jour + même heure)
        if (horaireRepository.existsByJourSemaineAndHeureAndStatusDelFalse(
                request.jourSemaine(), request.heure())) {
            throw new IllegalStateException("Un horaire identique (jour + heure) existe déjà.");
        }

        Horaire entity = horaireMapper.toEntity(request);
        if (entity.getStatusDel() == null) entity.setStatusDel(false);

        Horaire saved = horaireRepository.save(entity);
        return horaireMapper.toResponse(saved);
    }

    @Override
    public HoraireResponse update(UUID publicId, HoraireRequest request) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public de l'horaire n'a pas été reçu");
        }
        if (request == null) {
            throw new RequestNotFoundException("La requête d'horaire n'a pas été reçue");
        }
        validate(request);

        Horaire entity = horaireRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Horaire non trouvé avec l'identifiant public : " + publicId));

        if (Boolean.TRUE.equals(entity.getStatusDel())) {
            throw new EntityNotFoundException("Impossible de mettre à jour un horaire supprimé");
        }

        boolean sameJour = Objects.equals(entity.getJourSemaine(), request.jourSemaine());
        boolean sameHeure = Objects.equals(entity.getHeure(), request.heure());

        if ((!sameJour || !sameHeure) &&
                horaireRepository.existsByJourSemaineAndHeureAndStatusDelFalse(
                        request.jourSemaine(), request.heure())) {
            throw new IllegalStateException("Conflit : un horaire identique (jour + heure) existe déjà.");
        }

        entity.setJourSemaine(request.jourSemaine());
        entity.setHeure(request.heure());

        Horaire saved = horaireRepository.save(entity);
        return horaireMapper.toResponse(saved);
    }

    @Override
    public List<HoraireResponse> getAll() {
        // si tu veux lister uniquement les actifs :
        return horaireRepository.findByStatusDelFalse().stream()
                .map(horaireMapper::toResponse)
                .toList();

        // si tu préfères TOUT (y compris supprimés), remplace par:
        // return horaireRepository.findAll().stream().map(horaireMapper::toResponse).toList();
    }

    @Override
    public HoraireResponse getByPublicId(UUID publicId) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public de l'horaire n'a pas été reçu");
        }

        Horaire entity = horaireRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Horaire non trouvé avec l'identifiant public : " + publicId));

        if (Boolean.TRUE.equals(entity.getStatusDel())) {
            throw new EntityNotFoundException("Cet horaire a été supprimé");
        }

        return horaireMapper.toResponse(entity);
    }

    @Override
    public void delete(UUID publicId) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public de l'horaire n'a pas été reçu");
        }

        Horaire entity = horaireRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Horaire non trouvé avec l'identifiant public : " + publicId));

        entity.setStatusDel(true); // soft delete
        horaireRepository.save(entity);
    }

    // ---- Helpers
    private void validate(HoraireRequest r) {
        if (r.jourSemaine() == null) {
            throw new IllegalArgumentException("Le jour de semaine est requis.");
        }
        if (r.heure() == null) {
            throw new IllegalArgumentException("L'heure est requise.");
        }
    }
}
