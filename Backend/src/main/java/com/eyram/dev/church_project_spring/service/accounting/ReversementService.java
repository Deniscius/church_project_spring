package com.eyram.dev.church_project_spring.service.accounting;

import com.eyram.dev.church_project_spring.DTO.request.DemandeReversementRequest;
import com.eyram.dev.church_project_spring.DTO.request.ReversementDecisionRequest;
import com.eyram.dev.church_project_spring.DTO.response.CompteParoisseResponse;
import com.eyram.dev.church_project_spring.DTO.response.DemandeReversementResponse;
import com.eyram.dev.church_project_spring.entities.CompteParoisse;
import com.eyram.dev.church_project_spring.entities.DemandeReversement;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutReversement;
import com.eyram.dev.church_project_spring.repositories.CompteParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeReversementRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReversementService {

    private final DemandeReversementRepository demandeReversementRepository;
    private final CompteParoisseRepository compteParoisseRepository;
    private final ParoisseRepository paroisseRepository;
    private final ParishLedgerService parishLedgerService;
    private final TenantAccessService tenantAccessService;
    private final ReversementMailNotifier reversementMailNotifier;

    /**
     * Consultation pure : le compte n'est matérialisé qu'au premier mouvement
     * réel (crédit d'intention ou réservation de reversement). Une paroisse
     * encore sans écriture affiche donc un solde à zéro plutôt qu'une erreur.
     */
    @Transactional(readOnly = true)
    public CompteParoisseResponse getCompte(UUID paroissePublicId) {
        Paroisse paroisse = requireParoisse(paroissePublicId);
        tenantAccessService.checkParoisseAccess(paroisse);
        return compteParoisseRepository.findByParoisseAndStatusDelFalse(paroisse)
                .map(this::toCompteResponse)
                .orElseGet(() -> emptyCompteResponse(paroisse));
    }

    @Transactional
    public DemandeReversementResponse demander(DemandeReversementRequest request) {
        Paroisse paroisse = requireParoisse(request.paroissePublicId());
        tenantAccessService.checkParoisseAccess(paroisse);

        if (!StringUtils.hasText(paroisse.getIbanOrRib())) {
            throw new BusinessRuleException(
                    "Renseignez d'abord le RIB / compte bancaire de la paroisse avant de demander un reversement"
            );
        }

        DemandeReversement demande = new DemandeReversement();
        demande.setParoisse(paroisse);
        demande.setMontant(request.montant());
        demande.setStatut(StatutReversement.EN_ATTENTE);
        demande.setNomBanque(paroisse.getNomBanque());
        demande.setTitulaireCompte(paroisse.getTitulaireCompte());
        demande.setIbanOrRib(paroisse.getIbanOrRib());
        demande.setMotif(request.motif());
        demande.setStatusDel(false);
        demande = demandeReversementRepository.save(demande);

        parishLedgerService.holdForPayout(paroisse, request.montant(), "REV:" + demande.getPublicId());
        reversementMailNotifier.notifyComptablesNouvelleDemande(demande);
        return toResponse(demande);
    }

    @Transactional(readOnly = true)
    public List<DemandeReversementResponse> listAll() {
        return demandeReversementRepository.findByStatusDelFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeReversementResponse> listByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = requireParoisse(paroissePublicId);
        tenantAccessService.checkParoisseAccess(paroisse);
        return demandeReversementRepository.findByParoisseAndStatusDelFalseOrderByCreatedAtDesc(paroisse)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DemandeReversementResponse marquerPaye(UUID publicId, ReversementDecisionRequest request, String traitePar) {
        DemandeReversement demande = requireDemande(publicId);
        if (demande.getStatut() != StatutReversement.EN_ATTENTE) {
            throw new BusinessRuleException("Ce reversement n'est plus en attente");
        }
        if (!StringUtils.hasText(request.referenceVirement())) {
            throw new BusinessRuleException("La référence de virement est obligatoire");
        }
        parishLedgerService.confirmPayout(
                demande.getParoisse(),
                demande.getMontant(),
                "REV-PAYE:" + demande.getPublicId()
        );
        demande.setStatut(StatutReversement.PAYE);
        demande.setReferenceVirement(request.referenceVirement().trim());
        demande.setTraitePar(traitePar);
        demande.setTraiteAt(LocalDateTime.now());
        demande = demandeReversementRepository.save(demande);
        reversementMailNotifier.notifyParoisseDecision(demande);
        return toResponse(demande);
    }

    @Transactional
    public DemandeReversementResponse rejeter(UUID publicId, ReversementDecisionRequest request, String traitePar) {
        DemandeReversement demande = requireDemande(publicId);
        if (demande.getStatut() != StatutReversement.EN_ATTENTE) {
            throw new BusinessRuleException("Ce reversement n'est plus en attente");
        }
        parishLedgerService.releasePayoutHold(
                demande.getParoisse(),
                demande.getMontant(),
                "REV-REJET:" + demande.getPublicId()
        );
        demande.setStatut(StatutReversement.REJETE);
        if (request != null && StringUtils.hasText(request.motif())) {
            demande.setMotif(request.motif());
        }
        demande.setTraitePar(traitePar);
        demande.setTraiteAt(LocalDateTime.now());
        demande = demandeReversementRepository.save(demande);
        reversementMailNotifier.notifyParoisseDecision(demande);
        return toResponse(demande);
    }

    private DemandeReversement requireDemande(UUID publicId) {
        return demandeReversementRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de reversement introuvable"));
    }

    private Paroisse requireParoisse(UUID publicId) {
        return paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
    }

    private DemandeReversementResponse toResponse(DemandeReversement d) {
        return new DemandeReversementResponse(
                d.getPublicId(),
                d.getParoisse().getPublicId(),
                d.getParoisse().getNom(),
                d.getMontant(),
                d.getStatut(),
                d.getNomBanque(),
                d.getTitulaireCompte(),
                d.getIbanOrRib(),
                d.getMotif(),
                d.getReferenceVirement(),
                d.getTraitePar(),
                d.getTraiteAt(),
                d.getCreatedAt()
        );
    }

    private CompteParoisseResponse toCompteResponse(CompteParoisse c) {
        return new CompteParoisseResponse(
                c.getPublicId(),
                c.getParoisse().getPublicId(),
                c.getParoisse().getNom(),
                c.getSoldeDisponible(),
                c.getSoldeEnAttente()
        );
    }

    private CompteParoisseResponse emptyCompteResponse(Paroisse paroisse) {
        return new CompteParoisseResponse(
                null,
                paroisse.getPublicId(),
                paroisse.getNom(),
                0,
                0
        );
    }
}
