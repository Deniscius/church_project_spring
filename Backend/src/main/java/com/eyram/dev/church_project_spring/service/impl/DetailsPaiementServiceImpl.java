package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.DetailsPaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.DetailsPaiementResponse;
import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import com.eyram.dev.church_project_spring.entities.Facture;
import com.eyram.dev.church_project_spring.entities.TypePaiement;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.mappers.DetailsPaiementMapper;
import com.eyram.dev.church_project_spring.repositories.DetailsPaiementRepository;
import com.eyram.dev.church_project_spring.repositories.FactureRepository;
import com.eyram.dev.church_project_spring.repositories.TypePaiementRepository;
import com.eyram.dev.church_project_spring.service.DetailsPaiementService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DetailsPaiementServiceImpl implements DetailsPaiementService {

    private final DetailsPaiementRepository detailsPaiementRepository;
    private final TypePaiementRepository typePaiementRepository;
    private final FactureRepository factureRepository;
    private final DetailsPaiementMapper detailsPaiementMapper;

    @Override
    public DetailsPaiementResponse create(DetailsPaiementRequest request) {

        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(request.typePaiementPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type paiement introuvable"));

        Facture facture = factureRepository.findByPublicIdAndStatusDelFalse(request.facturePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));

        if (facture.getDemande().getTypePaiement() == null ||
                !facture.getDemande().getTypePaiement().getPublicId().equals(typePaiement.getPublicId())) {
            throw new IllegalArgumentException("Le type de paiement ne correspond pas à la demande");
        }

        detailsPaiementRepository.findByFacturePublicIdAndStatusDelFalse(request.facturePublicId())
                .ifPresent(existing -> {
                    throw new AlreadyExistException("Un détail paiement existe déjà pour cette facture");
                });

        DetailsPaiement detailsPaiement = detailsPaiementMapper.dtoToModel(request);
        detailsPaiement.setTypePaiement(typePaiement);
        detailsPaiement.setFacture(facture);

        DetailsPaiement detailsPaiementSave = detailsPaiementRepository.save(detailsPaiement);

        facture.setStatutPaiement(request.statutPaiement());
        facture.setDatePaiement(request.dateDetailsPaiement());
        factureRepository.save(facture);

        return detailsPaiementMapper.modelToDto(detailsPaiementSave);
    }

    @Override
    public DetailsPaiementResponse getByPublicId(UUID publicId) {

        DetailsPaiement detailsPaiement = detailsPaiementRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Détail paiement introuvable"));

        return detailsPaiementMapper.modelToDto(detailsPaiement);
    }

    @Override
    public List<DetailsPaiementResponse> getAll() {
        return detailsPaiementRepository.findAllByStatusDelFalse()
                .stream()
                .map(detailsPaiementMapper::modelToDto)
                .toList();
    }

    @Override
    public DetailsPaiementResponse update(UUID publicId, DetailsPaiementRequest request) {

        DetailsPaiement detailsPaiement = detailsPaiementRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Détail paiement introuvable"));

        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(request.typePaiementPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type paiement introuvable"));

        Facture facture = factureRepository.findByPublicIdAndStatusDelFalse(request.facturePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));

        if (facture.getDemande().getTypePaiement() == null ||
                !facture.getDemande().getTypePaiement().getPublicId().equals(typePaiement.getPublicId())) {
            throw new IllegalArgumentException("Le type de paiement ne correspond pas à la demande");
        }

        detailsPaiementRepository.findByFacturePublicIdAndStatusDelFalse(request.facturePublicId())
                .ifPresent(existing -> {
                    if (!existing.getPublicId().equals(publicId)) {
                        throw new AlreadyExistException("Un détail paiement existe déjà pour cette facture");
                    }
                });

        detailsPaiementMapper.dtoToModel(request, detailsPaiement);
        detailsPaiement.setTypePaiement(typePaiement);
        detailsPaiement.setFacture(facture);

        DetailsPaiement detailsPaiementUpdate = detailsPaiementRepository.save(detailsPaiement);

        facture.setStatutPaiement(request.statutPaiement());
        facture.setDatePaiement(request.dateDetailsPaiement());
        factureRepository.save(facture);

        return detailsPaiementMapper.modelToDto(detailsPaiementUpdate);
    }

    @Override
    public void delete(UUID publicId) {

        DetailsPaiement detailsPaiement = detailsPaiementRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Détail paiement introuvable"));

        detailsPaiement.setStatusDel(true);
        detailsPaiementRepository.save(detailsPaiement);

        Facture facture = detailsPaiement.getFacture();
        if (facture != null) {
            facture.setStatutPaiement(StatutPaiementEnum.NON_PAYE);
            facture.setDatePaiement(null);
            factureRepository.save(facture);
        }
    }
}