package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.FactureRequest;
import com.eyram.dev.church_project_spring.DTO.response.FactureResponse;
import com.eyram.dev.church_project_spring.DTO.response.PaiementReglementResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import com.eyram.dev.church_project_spring.entities.Facture;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.mappers.FactureMapper;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.DetailsPaiementRepository;
import com.eyram.dev.church_project_spring.repositories.FactureRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.FactureService;
import com.eyram.dev.church_project_spring.utils.BusinessCodeGenerator;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FactureServiceImpl implements FactureService {

    private final FactureRepository factureRepository;
    private final DemandeRepository demandeRepository;
    private final ParoisseRepository paroisseRepository;
    private final DetailsPaiementRepository detailsPaiementRepository;
    private final FactureMapper factureMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    public FactureResponse create(FactureRequest request) {

        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(request.demandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
        tenantAccessService.checkParoisseAccess(demande.getParoisse());

        factureRepository.findByDemandePublicIdAndStatusDelFalse(request.demandePublicId())
                .ifPresent(existing -> {
                    throw new AlreadyExistException("Une facture existe déjà pour cette demande");
                });

        Facture facture = factureMapper.dtoToModel(request);
        facture.setDemande(demande);
        facture.setMontant(demande.getMontant().setScale(0, java.math.RoundingMode.HALF_UP).intValue());

        if (request.refFacture() == null || request.refFacture().isBlank()) {
            facture.setRefFacture(generateRefFacture(demande));
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
        checkFactureAccess(facture);

        return withReglement(facture);
    }

    @Override
    public List<FactureResponse> getAll() {
        // tenantFilter Hibernate restreint déjà le périmètre paroisse.
        return withReglements(factureRepository.findAllActiveWithDemande());
    }

    @Override
    public List<FactureResponse> getByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        tenantAccessService.checkParoisseAccess(paroisse);
        return withReglements(factureRepository.findByParoissePublicIdWithDemande(paroissePublicId));
    }



    @Override
    public FactureResponse update(UUID publicId, FactureRequest request) {

        Facture facture = factureRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
        checkFactureAccess(facture);

        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(request.demandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
        tenantAccessService.checkParoisseAccess(demande.getParoisse());

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
        facture.setMontant(demande.getMontant().setScale(0, java.math.RoundingMode.HALF_UP).intValue());

        if (request.refFacture() == null || request.refFacture().isBlank()) {
            if (facture.getRefFacture() == null || facture.getRefFacture().isBlank()) {
                facture.setRefFacture(generateRefFacture(demande));
            }
        } else {
            facture.setRefFacture(request.refFacture());
        }

        Facture factureUpdate = factureRepository.save(facture);

        demande.setStatutPaiement(factureUpdate.getStatutPaiement());
        demandeRepository.save(demande);

        return factureMapper.modelToDto(factureUpdate);
    }

    @Override
    public void delete(UUID publicId) {

        Facture facture = factureRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
        checkFactureAccess(facture);

        detailsPaiementRepository.findByFacturePublicIdAndStatusDelFalse(facture.getPublicId())
                .ifPresent(details -> {
                    details.setStatusDel(true);
                    detailsPaiementRepository.save(details);
                });

        facture.setStatusDel(true);
        factureRepository.save(facture);
    }

    /** Charge les règlements en une requête : la liste des factures reste en O(1) requête. */
    private List<FactureResponse> withReglements(List<Facture> factures) {
        if (factures.isEmpty()) {
            return List.of();
        }
        Map<UUID, DetailsPaiement> parFacture = detailsPaiementRepository
                .findByFacture_PublicIdInAndStatusDelFalse(
                        factures.stream().map(Facture::getPublicId).toList()
                )
                .stream()
                .collect(Collectors.toMap(
                        details -> details.getFacture().getPublicId(),
                        details -> details,
                        (first, second) -> first
                ));

        return factures.stream()
                .map(facture -> factureMapper.modelToDto(facture)
                        .withReglement(toReglement(parFacture.get(facture.getPublicId()))))
                .toList();
    }

    private FactureResponse withReglement(Facture facture) {
        DetailsPaiement details = detailsPaiementRepository
                .findByFacturePublicIdAndStatusDelFalse(facture.getPublicId())
                .orElse(null);
        return factureMapper.modelToDto(facture).withReglement(toReglement(details));
    }

    private PaiementReglementResponse toReglement(DetailsPaiement details) {
        if (details == null) {
            return null;
        }
        return new PaiementReglementResponse(
                details.getDateDetailsPaiement(),
                details.getStatutPaiement(),
                details.getMontantCharge(),
                details.getMontantFrais(),
                details.getMontantFraisAgregeateur(),
                details.getMontantFraisPlateforme(),
                details.getMontantNet(),
                details.getProvider(),
                details.getIdTransaction(),
                details.getNumero()
        );
    }

    private String generateRefFacture(Demande demande) {
        String parishName = demande != null && demande.getParoisse() != null
                ? demande.getParoisse().getNom()
                : null;
        return BusinessCodeGenerator.unique(
                BusinessCodeGenerator.factureCode(parishName),
                ref -> factureRepository.findByRefFactureAndStatusDelFalse(ref).isPresent()
        );
    }


    @Override
    public FactureResponse getByCodeSuivie(String codeSuivie) {
        String code = BusinessCodeGenerator.normalizeDemandeTrackingCode(codeSuivie);
        Facture facture = factureRepository.findByDemandeCodeSuivieAndStatusDelFalse(code)
                .or(() -> factureRepository.findByDemandeCodeSuivieAndStatusDelFalse(
                        codeSuivie == null ? "" : codeSuivie.trim()))
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable pour ce code de suivi"));

        return withReglement(facture);
    }

    private void checkFactureAccess(Facture facture) {
        if (facture.getDemande() == null || facture.getDemande().getParoisse() == null) {
            throw new ResourceNotFoundException("Paroisse de la facture introuvable");
        }
        tenantAccessService.checkParoisseAccess(facture.getDemande().getParoisse());
    }


}
