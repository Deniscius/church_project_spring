package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.TypePaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypePaiementResponse;
import com.eyram.dev.church_project_spring.entities.TypePaiement;
import com.eyram.dev.church_project_spring.mappers.TypePaiementMapper;
import com.eyram.dev.church_project_spring.repositories.TypePaiementRepository;
import com.eyram.dev.church_project_spring.service.TypePaiementService;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import com.eyram.dev.church_project_spring.utils.exception.TrackingIdNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TypePaiementServiceImpl implements TypePaiementService {

    private final TypePaiementRepository typePaiementRepository;
    private final TypePaiementMapper typePaiementMapper;

    @Override
    public TypePaiementResponse create(TypePaiementRequest request) {
        if (request == null) {
            throw new RequestNotFoundException("La requête du type de paiement n'a pas été reçue");
        }

        // Vérifier si le libellé existe déjà
        typePaiementRepository.findByLibelle(request.libelle())
                .ifPresent(tp -> {
                    throw new IllegalArgumentException("Un type de paiement avec ce libellé existe déjà !");
                });

        TypePaiement typePaiement = typePaiementMapper.dtoToEntity(request);
        typePaiement.setStatusDel(false);

        TypePaiement saved = typePaiementRepository.save(typePaiement);
        return typePaiementMapper.entityToDto(saved);
    }

    @Override
    public TypePaiementResponse update(UUID publicId, TypePaiementRequest request) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public du type de paiement n'a pas été reçu");
        }
        if (request == null) {
            throw new RequestNotFoundException("La requête du type de paiement n'a pas été reçue");
        }

        TypePaiement typePaiement = typePaiementRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Type de paiement non trouvé : " + publicId));

        if (Boolean.TRUE.equals(typePaiement.getStatusDel())) {
            throw new EntityNotFoundException("Impossible de mettre à jour un type de paiement supprimé");
        }

        if (request.libelle() != null && !request.libelle().equals(typePaiement.getLibelle())) {
            typePaiement.setLibelle(request.libelle());
        }

        TypePaiement updated = typePaiementRepository.save(typePaiement);
        return typePaiementMapper.entityToDto(updated);
    }

    @Override
    public List<TypePaiementResponse> findAll() {
        return typePaiementRepository.findAll().stream()
                .map(typePaiementMapper::entityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public TypePaiementResponse findById(UUID publicId) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public du type de paiement n'a pas été reçu");
        }

        TypePaiement typePaiement = typePaiementRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Type de paiement non trouvé : " + publicId));

        if (Boolean.TRUE.equals(typePaiement.getStatusDel())) {
            throw new EntityNotFoundException("Ce type de paiement a été supprimé");
        }

        return typePaiementMapper.entityToDto(typePaiement);
    }

    @Override
    public void delete(UUID publicId) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public du type de paiement n'a pas été reçu");
        }

        TypePaiement typePaiement = typePaiementRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Type de paiement non trouvé : " + publicId));

        typePaiement.setStatusDel(true);
        typePaiementRepository.save(typePaiement);
    }
}
