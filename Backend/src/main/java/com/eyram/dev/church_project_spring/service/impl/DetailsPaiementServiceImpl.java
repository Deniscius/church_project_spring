package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.DetailsPaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.CaisseEncaissementResponse;
import com.eyram.dev.church_project_spring.DTO.response.CaisseResumeResponse;
import com.eyram.dev.church_project_spring.DTO.response.DetailsPaiementResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import com.eyram.dev.church_project_spring.entities.Facture;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.TypePaiement;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.mappers.DetailsPaiementMapper;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.DetailsPaiementRepository;
import com.eyram.dev.church_project_spring.repositories.FactureRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.TypePaiementRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.DemandePaymentEligibilityService;
import com.eyram.dev.church_project_spring.service.DetailsPaiementService;
import com.eyram.dev.church_project_spring.service.accounting.ParishLedgerService;
import com.eyram.dev.church_project_spring.service.payment.FedaPayPaymentService;
import com.eyram.dev.church_project_spring.utils.BusinessCodeGenerator;
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
public class DetailsPaiementServiceImpl implements DetailsPaiementService {

    private final DetailsPaiementRepository detailsPaiementRepository;
    private final TypePaiementRepository typePaiementRepository;
    private final FactureRepository factureRepository;
    private final DemandeRepository demandeRepository;
    private final ParoisseRepository paroisseRepository;
    private final DetailsPaiementMapper detailsPaiementMapper;
    private final TenantAccessService tenantAccessService;
    private final FedaPayPaymentService fedaPayPaymentService;
    private final ParishLedgerService parishLedgerService;
    private final DemandePaymentEligibilityService demandePaymentEligibilityService;

    @Override
    public DetailsPaiementResponse create(DetailsPaiementRequest request) {

        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(request.typePaiementPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type paiement introuvable"));

        Facture facture = factureRepository.findByPublicIdAndStatusDelFalse(request.facturePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
        checkFactureAccess(facture);
        validatePaymentAmount(facture, request.montant());
        demandePaymentEligibilityService.assertCanStartPayment(facture.getDemande());

        if (typePaiement.getMode() != ModePaiement.ESPECES) {
            throw new BusinessRuleException(
                    "Un paiement électronique doit être confirmé par le fournisseur de paiement"
            );
        }

        if (facture.getDemande() == null
                || facture.getDemande().getTypePaiement() == null
                || !facture.getDemande().getTypePaiement().getPublicId().equals(typePaiement.getPublicId())) {
            throw new IllegalArgumentException("Le type de paiement ne correspond pas à la demande");
        }

        DetailsPaiement existingDetails = detailsPaiementRepository
                .findByFacturePublicId(request.facturePublicId())
                .orElse(null);

        // Le statut est fixé côté serveur : un paiement enregistré = PAYE.
        StatutPaiementEnum effectiveStatus = StatutPaiementEnum.PAYE;

        if (existingDetails != null) {
            if (Boolean.FALSE.equals(existingDetails.getStatusDel())) {
                throw new AlreadyExistException("Un détail paiement existe déjà pour cette facture");
            }

            existingDetails.setStatusDel(false);
            existingDetails.setDateDetailsPaiement(request.dateDetailsPaiement());
            existingDetails.setMontant(request.montant());
            existingDetails.setNumero(request.numero());
            existingDetails.setStatutPaiement(effectiveStatus);
            existingDetails.setTypePaiement(typePaiement);
            existingDetails.setFacture(facture);

            if (request.idTransaction() == null || request.idTransaction().isBlank()) {
                existingDetails.setIdTransaction(generateTransactionId(facture));
            } else {
                existingDetails.setIdTransaction(request.idTransaction());
            }

            fedaPayPaymentService.applyFeeSnapshot(existingDetails, typePaiement.getMode(), request.montant());
            DetailsPaiement reactivatedDetails = detailsPaiementRepository.save(existingDetails);
            syncPaymentStatus(facture, effectiveStatus, request.dateDetailsPaiement());
            creditParishIfPaid(reactivatedDetails, facture, effectiveStatus);
            return detailsPaiementMapper.modelToDto(reactivatedDetails);
        }

        DetailsPaiement detailsPaiement = detailsPaiementMapper.dtoToModel(request);
        detailsPaiement.setTypePaiement(typePaiement);
        detailsPaiement.setFacture(facture);
        detailsPaiement.setStatutPaiement(effectiveStatus);

        if (request.idTransaction() == null || request.idTransaction().isBlank()) {
            detailsPaiement.setIdTransaction(generateTransactionId(facture));
        }

        fedaPayPaymentService.applyFeeSnapshot(detailsPaiement, typePaiement.getMode(), request.montant());
        DetailsPaiement savedDetailsPaiement = detailsPaiementRepository.save(detailsPaiement);
        syncPaymentStatus(facture, effectiveStatus, request.dateDetailsPaiement());
        creditParishIfPaid(savedDetailsPaiement, facture, effectiveStatus);

        return detailsPaiementMapper.modelToDto(savedDetailsPaiement);
    }

    @Override
    public DetailsPaiementResponse getByPublicId(UUID publicId) {
        DetailsPaiement detailsPaiement = detailsPaiementRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Détail paiement introuvable"));
        checkFactureAccess(detailsPaiement.getFacture());

        if (detailsPaiement.getStatutPaiement() == StatutPaiementEnum.PAYE) {
            throw new BusinessRuleException(
                    "Un paiement confirmé est immuable. Utilisez une procédure de remboursement."
            );
        }

        return detailsPaiementMapper.modelToDto(detailsPaiement);
    }

    @Override
    public List<DetailsPaiementResponse> getAll() {
        // tenantFilter Hibernate restreint déjà le périmètre paroisse.
        return detailsPaiementRepository.findAllActiveWithAssociations()
                .stream()
                .map(detailsPaiementMapper::modelToDto)
                .toList();
    }

    @Override
    public DetailsPaiementResponse update(UUID publicId, DetailsPaiementRequest request) {

        DetailsPaiement detailsPaiement = detailsPaiementRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Détail paiement introuvable"));
        checkFactureAccess(detailsPaiement.getFacture());

        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(request.typePaiementPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type paiement introuvable"));

        Facture facture = factureRepository.findByPublicIdAndStatusDelFalse(request.facturePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
        checkFactureAccess(facture);
        validatePaymentAmount(facture, request.montant());

        if (facture.getDemande() == null
                || facture.getDemande().getTypePaiement() == null
                || !facture.getDemande().getTypePaiement().getPublicId().equals(typePaiement.getPublicId())) {
            throw new IllegalArgumentException("Le type de paiement ne correspond pas à la demande");
        }

        if (!detailsPaiement.getFacture().getPublicId().equals(facture.getPublicId())) {
            throw new BusinessRuleException("Un paiement ne peut pas être réaffecté à une autre facture");
        }

        String transactionId = detailsPaiement.getIdTransaction();
        StatutPaiementEnum currentStatus = detailsPaiement.getStatutPaiement();
        LocalDateTime paymentDate = detailsPaiement.getDateDetailsPaiement();

        detailsPaiementMapper.dtoToModel(request, detailsPaiement);
        detailsPaiement.setTypePaiement(typePaiement);
        detailsPaiement.setFacture(facture);
        detailsPaiement.setIdTransaction(transactionId);
        detailsPaiement.setStatutPaiement(currentStatus);
        detailsPaiement.setDateDetailsPaiement(paymentDate);
        detailsPaiement.setMontant(facture.getMontant());

        fedaPayPaymentService.applyFeeSnapshot(detailsPaiement, typePaiement.getMode(), facture.getMontant());
        DetailsPaiement updatedDetailsPaiement = detailsPaiementRepository.save(detailsPaiement);
        syncPaymentStatus(facture, currentStatus, paymentDate);

        return detailsPaiementMapper.modelToDto(updatedDetailsPaiement);
    }

    @Override
    public void delete(UUID publicId) {

        DetailsPaiement detailsPaiement = detailsPaiementRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Détail paiement introuvable"));
        checkFactureAccess(detailsPaiement.getFacture());

        if (detailsPaiement.getStatutPaiement() == StatutPaiementEnum.PAYE) {
            throw new BusinessRuleException(
                    "Un paiement confirmé ne peut pas être supprimé. Utilisez une procédure de remboursement."
            );
        }

        detailsPaiement.setStatusDel(true);
        detailsPaiementRepository.save(detailsPaiement);

        Facture facture = detailsPaiement.getFacture();
        if (facture != null) {
            syncPaymentStatus(facture, StatutPaiementEnum.NON_PAYE, null);
        }
    }

    @Override
    public DetailsPaiementResponse encaisserCaisse(UUID demandePublicId) {
        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(demandePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
        tenantAccessService.checkParoisseAccess(demande.getParoisse());

        if (demande.getStatutPaiement() == StatutPaiementEnum.PAYE) {
            throw new BusinessRuleException("Cette demande est déjà payée");
        }
        demandePaymentEligibilityService.assertCanStartPayment(demande);

        TypePaiement especes = typePaiementRepository.findByModeAndStatusDelFalse(ModePaiement.ESPECES)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Le type de paiement Espèces n'est pas configuré"
                ));

        // Le fidèle peut avoir choisi un mode en ligne puis payer au secrétariat.
        demande.setTypePaiement(especes);
        demandeRepository.save(demande);

        Facture facture = factureRepository.findByDemandePublicIdAndStatusDelFalse(demandePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable pour cette demande"));

        DetailsPaiement existing = detailsPaiementRepository
                .findByFacturePublicIdAndStatusDelFalse(facture.getPublicId())
                .orElse(null);
        if (existing != null && existing.getStatutPaiement() == StatutPaiementEnum.PAYE) {
            throw new BusinessRuleException("Un paiement est déjà enregistré pour cette facture");
        }

        LocalDateTime now = LocalDateTime.now();
        DetailsPaiement details = existing != null ? existing : new DetailsPaiement();
        details.setStatusDel(false);
        details.setDateDetailsPaiement(now);
        details.setMontant(facture.getMontant());
        details.setStatutPaiement(StatutPaiementEnum.PAYE);
        details.setTypePaiement(especes);
        details.setFacture(facture);
        details.setNumero(demande.getCodeSuivie() != null ? demande.getCodeSuivie() : "CAISSE");
        details.setProvider(FedaPayPaymentService.PROVIDER_CAISSE_LOCALE);
        var encaisseur = tenantAccessService.getCurrentUser();
        details.setEncaisseurNom(encaisseur.getFullName() != null && !encaisseur.getFullName().isBlank()
                ? encaisseur.getFullName()
                : encaisseur.getUsername());
        details.setEncaisseurUserPublicId(encaisseur.getPublicId());
        if (details.getIdTransaction() == null || details.getIdTransaction().isBlank()
                || (existing != null && FedaPayPaymentService.PROVIDER_FEDAPAY.equals(existing.getProvider()))) {
            details.setIdTransaction(generateTransactionId(facture));
        }
        details.setPaymentUrl(null);

        fedaPayPaymentService.applyFeeSnapshot(details, ModePaiement.ESPECES, facture.getMontant());
        details.setProvider(FedaPayPaymentService.PROVIDER_CAISSE_LOCALE);

        DetailsPaiement saved = detailsPaiementRepository.save(details);
        syncPaymentStatus(facture, StatutPaiementEnum.PAYE, now);
        // Pas de creditParishIfPaid : l'argent reste en caisse paroisse.
        return detailsPaiementMapper.modelToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CaisseResumeResponse resumeCaisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        tenantAccessService.checkParoisseAccess(paroisse);

        List<DetailsPaiement> lignes = detailsPaiementRepository
                .findCaisseLocaleByParoisse(paroissePublicId, ModePaiement.ESPECES);

        LocalDate today = LocalDate.now();
        int total = 0;
        int totalDuJour = 0;
        int totalDuMois = 0;
        List<CaisseEncaissementResponse> encaissements = new java.util.ArrayList<>();

        for (DetailsPaiement dp : lignes) {
            int montant = dp.getMontant() != null ? dp.getMontant() : 0;
            total += montant;
            LocalDateTime date = dp.getDateDetailsPaiement();
            if (date != null) {
                if (date.toLocalDate().equals(today)) {
                    totalDuJour += montant;
                }
                if (date.getYear() == today.getYear() && date.getMonth() == today.getMonth()) {
                    totalDuMois += montant;
                }
            }
            Demande demande = dp.getFacture() != null ? dp.getFacture().getDemande() : null;
            String fidele = demande == null ? "—"
                    : com.eyram.dev.church_project_spring.utils.FideleNameUtils.format(
                            demande.getPrenomFidele(), demande.getNomFidele());
            encaissements.add(new CaisseEncaissementResponse(
                    dp.getPublicId(),
                    date,
                    montant,
                    demande != null ? demande.getCodeSuivie() : null,
                    demande != null ? demande.getIntention() : null,
                    fidele,
                    dp.getFacture() != null ? dp.getFacture().getRefFacture() : null,
                    dp.getIdTransaction(),
                    dp.getEncaisseurNom()
            ));
        }

        return new CaisseResumeResponse(total, totalDuJour, totalDuMois, encaissements.size(), encaissements);
    }

    private void creditParishIfPaid(DetailsPaiement details, Facture facture, StatutPaiementEnum statut) {
        if (statut != StatutPaiementEnum.PAYE || facture == null || facture.getDemande() == null) {
            return;
        }
        // Espèces / caisse locale : la paroisse encaisse elle-même, hors circuit de virement.
        ModePaiement mode = details.getTypePaiement() != null ? details.getTypePaiement().getMode() : null;
        if (mode == ModePaiement.ESPECES
                || FedaPayPaymentService.PROVIDER_CAISSE_LOCALE.equals(details.getProvider())) {
            return;
        }
        Demande demande = facture.getDemande();
        if (demande.getParoisse() == null) {
            return;
        }
        int credit = details.getMontantNet() != null ? details.getMontantNet() : details.getMontant();
        String ref = "MANUAL:" + details.getIdTransaction();
        parishLedgerService.creditMesse(demande.getParoisse(), credit, demande.getCodeSuivie(), ref);
    }

    private void syncPaymentStatus(Facture facture, StatutPaiementEnum statut, java.time.LocalDateTime datePaiement) {
        facture.setStatutPaiement(statut);
        facture.setDatePaiement(datePaiement);
        factureRepository.save(facture);

        Demande demande = facture.getDemande();
        if (demande != null) {
            demande.setStatutPaiement(statut);
            demandeRepository.save(demande);
        }
    }

    private void validatePaymentAmount(Facture facture, Integer montant) {
        if (!Objects.equals(facture.getMontant(), montant)) {
            throw new BusinessRuleException(
                    "Le montant du paiement (" + montant + ") ne correspond pas au montant de la facture ("
                            + facture.getMontant() + ")"
            );
        }
    }

    private String generateTransactionId(Facture facture) {
        String parishName = null;
        if (facture != null
                && facture.getDemande() != null
                && facture.getDemande().getParoisse() != null) {
            parishName = facture.getDemande().getParoisse().getNom();
        }
        return BusinessCodeGenerator.unique(
                BusinessCodeGenerator.transactionCode(parishName),
                detailsPaiementRepository::existsByIdTransactionAndStatusDelFalse
        );
    }

    private boolean canAccessFacture(Facture facture) {
        return facture != null
                && facture.getDemande() != null
                && facture.getDemande().getParoisse() != null
                && tenantAccessService.canAccessParoisse(facture.getDemande().getParoisse());
    }

    private void checkFactureAccess(Facture facture) {
        if (!canAccessFacture(facture)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé à cette facture"
            );
        }
    }
}
