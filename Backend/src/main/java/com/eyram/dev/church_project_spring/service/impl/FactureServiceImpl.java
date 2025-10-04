package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.FactureResquest;
import com.eyram.dev.church_project_spring.DTO.response.FactureResponse;
import com.eyram.dev.church_project_spring.entities.Facture;
import com.eyram.dev.church_project_spring.repository.FactureRepository;
import com.eyram.dev.church_project_spring.service.FactureService;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import com.eyram.dev.church_project_spring.utils.exception.TrackingIdNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class FactureServiceImpl implements FactureService {

    private final FactureRepository factureRepository;

    @Override
    public FactureResponse create(FactureResquest request) {
        if (request == null) {
            throw new RequestNotFoundException("La requête de facture n'a pas été reçue");
        }
        validate(request);

        Facture entity = new Facture();
        apply(entity, request);
        if (entity.getStatusDel() == null) entity.setStatusDel(false);

        Facture saved = factureRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    public List<Facture> findAll() {

        return factureRepository.findByStatusDelFalse();
    }

    @Override
    public Facture findByPublicId(UUID publicId) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public de la facture n'a pas été reçu");
        }
        Facture entity = factureRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Facture non trouvée avec l'identifiant public : " + publicId));

        if (Boolean.TRUE.equals(entity.getStatusDel())) {
            throw new EntityNotFoundException("Cette facture a été supprimée");
        }
        return entity;
    }

    @Override
    public FactureResponse update(UUID publicId, FactureResquest request) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public de la facture n'a pas été reçu");
        }
        if (request == null) {
            throw new RequestNotFoundException("La requête de facture n'a pas été reçue");
        }
        validate(request);

        Facture entity = factureRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Facture non trouvée avec l'identifiant public : " + publicId));

        if (Boolean.TRUE.equals(entity.getStatusDel())) {
            throw new EntityNotFoundException("Impossible de mettre à jour une facture supprimée");
        }

        apply(entity, request);
        Facture saved = factureRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    public void delete(UUID publicId) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public de la facture n'a pas été reçu");
        }
        Facture entity = factureRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Facture non trouvée avec l'identifiant public : " + publicId));

        entity.setStatusDel(true); // soft delete
        factureRepository.save(entity);
    }


    private void apply(Facture e, FactureResquest r) {
        e.setDatePaiement(r.datePaiement());
        e.setMontant(r.montant());
        e.setDateProduction(r.dateProduction());

    }

    private FactureResponse toResponse(Facture e) {
        return new FactureResponse(
                e.getPublicId(),
                e.getDatePaiement(),
                e.getMontant(),
                e.getDateProduction(),
                e.getStatusDel()
        );
    }

    private void validate(FactureResquest r) {
        if (r.montant() == null) {
            throw new IllegalArgumentException("Le montant est requis.");
        }
        if (r.montant() < 0) {
            throw new IllegalArgumentException("Le montant ne peut pas être négatif.");
        }
        // datePaiement / dateProduction : facultatives ou obligatoires ? adapte selon ton besoin
        // if (r.datePaiement() == null) throw new IllegalArgumentException("La date de paiement est requise.");
        // if (r.dateProduction() == null) throw new IllegalArgumentException("La date de production est requise.");
    }
}
