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

        if (facture.getDemande() == null
                || facture.getDemande().getTypePaiement() == null
                || !facture.getDemande().getTypePaiement().getPublicId().equals(typePaiement.getPublicId())) {
            throw new IllegalArgumentException("Le type de paiement ne correspond pas à la demande");
        }

        DetailsPaiement existingDetails = detailsPaiementRepository
                .findByFacturePublicId(request.facturePublicId())
                .orElse(null);

        if (existingDetails != null) {
            if (Boolean.FALSE.equals(existingDetails.getStatusDel())) {
                throw new AlreadyExistException("Un détail paiement existe déjà pour cette facture");
            }

            existingDetails.setStatusDel(false);
            existingDetails.setDateDetailsPaiement(request.dateDetailsPaiement());
            existingDetails.setMontant(request.montant());
            existingDetails.setNumero(request.numero());
            existingDetails.setStatutPaiement(request.statutPaiement());
            existingDetails.setTypePaiement(typePaiement);
            existingDetails.setFacture(facture);

            if (request.idTransaction() == null || request.idTransaction().isBlank()) {
                existingDetails.setIdTransaction(generateTransactionId());
            } else {
                existingDetails.setIdTransaction(request.idTransaction());
            }

            DetailsPaiement reactivatedDetails = detailsPaiementRepository.save(existingDetails);

            facture.setStatutPaiement(request.statutPaiement());
            facture.setDatePaiement(request.dateDetailsPaiement());
            factureRepository.save(facture);

            return detailsPaiementMapper.modelToDto(reactivatedDetails);
        }

        DetailsPaiement detailsPaiement = detailsPaiementMapper.dtoToModel(request);
        detailsPaiement.setTypePaiement(typePaiement);
        detailsPaiement.setFacture(facture);

        if (request.idTransaction() == null || request.idTransaction().isBlank()) {
            detailsPaiement.setIdTransaction(generateTransactionId());
        }

        DetailsPaiement savedDetailsPaiement = detailsPaiementRepository.save(detailsPaiement);

        facture.setStatutPaiement(request.statutPaiement());
        facture.setDatePaiement(request.dateDetailsPaiement());
        factureRepository.save(facture);

        return detailsPaiementMapper.modelToDto(savedDetailsPaiement);
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

        if (facture.getDemande() == null
                || facture.getDemande().getTypePaiement() == null
                || !facture.getDemande().getTypePaiement().getPublicId().equals(typePaiement.getPublicId())) {
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

        if (detailsPaiement.getIdTransaction() == null || detailsPaiement.getIdTransaction().isBlank()) {
            detailsPaiement.setIdTransaction(generateTransactionId());
        }

        DetailsPaiement updatedDetailsPaiement = detailsPaiementRepository.save(detailsPaiement);

        facture.setStatutPaiement(request.statutPaiement());
        facture.setDatePaiement(request.dateDetailsPaiement());
        factureRepository.save(facture);

        return detailsPaiementMapper.modelToDto(updatedDetailsPaiement);
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

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}