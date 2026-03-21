package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.FactureRequest;
import com.eyram.dev.church_project_spring.DTO.response.FactureResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.Facture;
import com.eyram.dev.church_project_spring.mappers.FactureMapper;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.FactureRepository;
import com.eyram.dev.church_project_spring.service.FactureService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FactureServiceImpl implements FactureService {

    private final FactureRepository factureRepository;
    private final DemandeRepository demandeRepository;
    private final FactureMapper factureMapper;

    @Override
    public FactureResponse create(FactureRequest request) {

        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(request.demandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        factureRepository.findByDemandePublicIdAndStatusDelFalse(request.demandePublicId())
                .ifPresent(existing -> {
                    throw new AlreadyExistException("Une facture existe déjà pour cette demande");
                });

        Facture facture = factureMapper.dtoToModel(request);
        facture.setDemande(demande);
        facture.setMontant(demande.getMontant().intValue());

        if (request.refFacture() == null || request.refFacture().isBlank()) {
            facture.setRefFacture(generateRefFacture());
        } else {
            factureRepository.findByRefFactureAndStatusDelFalse(request.refFacture())
                    .ifPresent(existing -> {
                        throw new AlreadyExistException("Cette référence facture existe déjà");
                    });
            facture.setRefFacture(request.refFacture());
        }

        Facture factureSave = factureRepository.save(facture);

        return factureMapper.modelToDto(factureSave);
    }

    @Override
    public FactureResponse getByPublicId(UUID publicId) {

        Facture facture = factureRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));

        return factureMapper.modelToDto(facture);
    }

    @Override
    public List<FactureResponse> getAll() {
        return factureRepository.findAllByStatusDelFalse()
                .stream()
                .map(factureMapper::modelToDto)
                .toList();
    }



    @Override
    public FactureResponse update(UUID publicId, FactureRequest request) {

        Facture facture = factureRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));

        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(request.demandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        factureRepository.findByDemandePublicIdAndStatusDelFalse(request.demandePublicId())
                .ifPresent(existing -> {
                    if (!existing.getPublicId().equals(publicId)) {
                        throw new AlreadyExistException("Une facture existe déjà pour cette demande");
                    }
                });

        if (request.refFacture() != null && !request.refFacture().isBlank()) {
            factureRepository.findByRefFactureAndStatusDelFalse(request.refFacture())
                    .ifPresent(existing -> {
                        if (!existing.getPublicId().equals(publicId)) {
                            throw new AlreadyExistException("Cette référence facture existe déjà");
                        }
                    });
        }

        factureMapper.dtoToModel(request, facture);
        facture.setDemande(demande);
        facture.setMontant(demande.getMontant().intValue());

        if (request.refFacture() == null || request.refFacture().isBlank()) {
            if (facture.getRefFacture() == null || facture.getRefFacture().isBlank()) {
                facture.setRefFacture(generateRefFacture());
            }
        } else {
            facture.setRefFacture(request.refFacture());
        }

        Facture factureUpdate = factureRepository.save(facture);

        return factureMapper.modelToDto(factureUpdate);
    }

    @Override
    public void delete(UUID publicId) {

        Facture facture = factureRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));

        facture.setStatusDel(true);
        factureRepository.save(facture);
    }

    private String generateRefFacture() {
        return "FAC" + LocalDateTime.now().getYear() + "-" + System.currentTimeMillis();
    }


    @Override
    public FactureResponse getByCodeSuivie(String codeSuivie) {
        Facture facture = factureRepository.findByDemandeCodeSuivieAndStatusDelFalse(codeSuivie)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable pour ce code de suivi"));

        return factureMapper.modelToDto(facture);
    }


}