package com.eyram.dev.church_project_spring.service.accounting;

import com.eyram.dev.church_project_spring.entities.CompteParoisse;
import com.eyram.dev.church_project_spring.entities.EcritureComptable;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.TypeEcriture;
import com.eyram.dev.church_project_spring.repositories.CompteParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.EcritureComptableRepository;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ParishLedgerService {

    private final CompteParoisseRepository compteParoisseRepository;
    private final EcritureComptableRepository ecritureComptableRepository;

    @Transactional
    public CompteParoisse ensureCompte(Paroisse paroisse) {
        return compteParoisseRepository.findByParoisseAndStatusDelFalse(paroisse)
                .orElseGet(() -> {
                    CompteParoisse compte = new CompteParoisse();
                    compte.setParoisse(paroisse);
                    compte.setSoldeDisponible(0);
                    compte.setSoldeEnAttente(0);
                    compte.setStatusDel(false);
                    return compteParoisseRepository.save(compte);
                });
    }

    @Transactional
    public void creditMesse(Paroisse paroisse, int montant, String codeSuivie, String referenceExterne) {
        if (montant <= 0 || paroisse == null) {
            return;
        }
        if (StringUtils.hasText(referenceExterne)
                && ecritureComptableRepository.existsByReferenceExterneAndStatusDelFalse(referenceExterne)) {
            return;
        }
        CompteParoisse compte = ensureCompte(paroisse);
        compte.setSoldeDisponible(compte.getSoldeDisponible() + montant);
        compteParoisseRepository.save(compte);

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setParoisse(paroisse);
        ecriture.setTypeEcriture(TypeEcriture.CREDIT_MESSE);
        ecriture.setMontant(montant);
        ecriture.setLibelle("Crédit intention de messe");
        ecriture.setDemandeCodeSuivie(codeSuivie);
        ecriture.setReferenceExterne(referenceExterne);
        ecriture.setStatusDel(false);
        ecritureComptableRepository.save(ecriture);
    }

    @Transactional
    public void holdForPayout(Paroisse paroisse, int montant, String reference) {
        CompteParoisse compte = ensureCompte(paroisse);
        if (compte.getSoldeDisponible() < montant) {
            throw new BusinessRuleException("Solde disponible insuffisant pour ce reversement");
        }
        compte.setSoldeDisponible(compte.getSoldeDisponible() - montant);
        compte.setSoldeEnAttente(compte.getSoldeEnAttente() + montant);
        compteParoisseRepository.save(compte);

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setParoisse(paroisse);
        ecriture.setTypeEcriture(TypeEcriture.DEBIT_REVERSEMENT_HOLD);
        ecriture.setMontant(montant);
        ecriture.setLibelle("Réservation reversement");
        ecriture.setReferenceExterne(reference);
        ecriture.setStatusDel(false);
        ecritureComptableRepository.save(ecriture);
    }

    @Transactional
    public void confirmPayout(Paroisse paroisse, int montant, String reference) {
        CompteParoisse compte = ensureCompte(paroisse);
        if (compte.getSoldeEnAttente() < montant) {
            throw new BusinessRuleException("Solde en attente incohérent pour confirmer le virement");
        }
        compte.setSoldeEnAttente(compte.getSoldeEnAttente() - montant);
        compteParoisseRepository.save(compte);

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setParoisse(paroisse);
        ecriture.setTypeEcriture(TypeEcriture.DEBIT_REVERSEMENT);
        ecriture.setMontant(montant);
        ecriture.setLibelle("Reversement exécuté");
        ecriture.setReferenceExterne(reference);
        ecriture.setStatusDel(false);
        ecritureComptableRepository.save(ecriture);
    }

    @Transactional
    public void releasePayoutHold(Paroisse paroisse, int montant, String reference) {
        CompteParoisse compte = ensureCompte(paroisse);
        if (compte.getSoldeEnAttente() < montant) {
            throw new BusinessRuleException("Solde en attente incohérent pour annuler le reversement");
        }
        compte.setSoldeEnAttente(compte.getSoldeEnAttente() - montant);
        compte.setSoldeDisponible(compte.getSoldeDisponible() + montant);
        compteParoisseRepository.save(compte);

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setParoisse(paroisse);
        ecriture.setTypeEcriture(TypeEcriture.CREDIT_REVERSEMENT_REJECT);
        ecriture.setMontant(montant);
        ecriture.setLibelle("Reversement rejeté — solde rétabli");
        ecriture.setReferenceExterne(reference);
        ecriture.setStatusDel(false);
        ecritureComptableRepository.save(ecriture);
    }
}
