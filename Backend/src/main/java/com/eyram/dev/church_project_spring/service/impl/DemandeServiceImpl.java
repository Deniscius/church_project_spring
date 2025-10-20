package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.Fidele;
import com.eyram.dev.church_project_spring.enums.StatusValidationEnum;
import com.eyram.dev.church_project_spring.mappers.DemandeMapper;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.FideleRepository;
import com.eyram.dev.church_project_spring.service.DemandeService;
import com.eyram.dev.church_project_spring.service.PricingService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DemandeServiceImpl implements DemandeService {

    private final DemandeRepository demandeRepository;
    private final FideleRepository fideleRepository;     // pour getAllByFidele
    private final DemandeMapper demandeMapper;           // ton mapper (DTO<->Entity)
    private final PricingService pricingService;         // calcule les prix

    @Override
    public DemandeResponse create(DemandeRequest request) {
        if (request == null) throw new IllegalArgumentException("Requête non reçue");

        // 1) Mapper la requête -> entité (le mapper pose relations si tu as gardé ce choix)
        Demande entity = demandeMapper.dtoToModel(request);

        // 2) Valeurs par défaut/cohérence
        if (entity.getPublicId() == null) entity.setPublicId(UUID.randomUUID());
        if (entity.getStatusDel() == null) entity.setStatusDel(false);
        if (entity.getDateDemande() == null) entity.setDateDemande(LocalDate.now());
        if (entity.getStatusValidationEnum() == null) entity.setStatusValidationEnum(StatusValidationEnum.EN_ATTENTE);

        // 3) Calcul du prix total
        int total = pricingService.totalFor(
                entity.getTypeDemandeEnum(),
                entity.getDureeMesse(),
                entity.getDates()
        );
        entity.setPrixTotal(total);

        // 4) Persist & return
        Demande saved = demandeRepository.save(entity);
        return demandeMapper.modelToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DemandeResponse getByPublicId(UUID publicId) {
        if (publicId == null) throw new IllegalArgumentException("publicId manquant");
        Demande d = demandeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        if (Boolean.TRUE.equals(d.getStatusDel())) {
            throw new EntityNotFoundException("Demande supprimée");
        }
        return demandeMapper.modelToDto(d);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeResponse> getAll() {
        // Si tu as la méthode repo: findByStatusDelFalse()
        // return demandeRepository.findByStatusDelFalse().stream().map(demandeMapper::modelToDto).toList();

        // Sinon, fallback: filtrer en mémoire
        return demandeRepository.findAll().stream()
                .filter(d -> !Boolean.TRUE.equals(d.getStatusDel()))
                .map(demandeMapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeResponse> getAllByFidele(UUID fidelePublicId) {
        if (fidelePublicId == null) throw new IllegalArgumentException("fidelePublicId manquant");
        Fidele f = fideleRepository.findByPublicId(fidelePublicId)
                .orElseThrow(() -> new EntityNotFoundException("Fidèle introuvable"));

        // Si tu as la méthode repo: findByFideleAndStatusDelFalse(f)
        // return demandeRepository.findByFideleAndStatusDelFalse(f).stream().map(demandeMapper::modelToDto).toList();

        return demandeRepository.findByFidele(f).stream()
                .filter(d -> !Boolean.TRUE.equals(d.getStatusDel()))
                .map(demandeMapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeResponse> getAllByStatus(StatusValidationEnum status) {
        if (status == null) throw new IllegalArgumentException("status manquant");

        // Si tu ajoutes une méthode repo dédiée, utilise-la. Sinon filtre en mémoire :
        return demandeRepository.findAll().stream()
                .filter(d -> !Boolean.TRUE.equals(d.getStatusDel()))
                .filter(d -> d.getStatusValidationEnum() == status)
                .map(demandeMapper::modelToDto)
                .toList();
    }

    @Override
    public DemandeResponse update(UUID publicId, DemandeRequest request) {
        if (publicId == null) throw new IllegalArgumentException("publicId manquant");
        if (request == null) throw new IllegalArgumentException("Requête manquante");

        Demande d = demandeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        if (Boolean.TRUE.equals(d.getStatusDel())) {
            throw new IllegalStateException("Impossible de modifier une demande supprimée");
        }
        if (d.getStatusValidationEnum() != StatusValidationEnum.EN_ATTENTE) {
            throw new IllegalStateException("Seules les demandes EN_ATTENTE sont modifiables");
        }

        // Appliquer les champs depuis la request (adapte selon ton DemandeRequest final)
        d.setIntention(request.intention());
        if (request.dateDemande() != null) d.setDateDemande(request.dateDemande());

        // Si ton DemandeRequest contient les nouveaux champs :
        // d.setTypeDemandeEnum(request.typeDemandeEnum());
        // d.setDureeMesse(request.dureeMesse());
        // d.setDates(request.dates());

        // Recalcul du prix (si type/durée/dates ont pu changer)
        int total = pricingService.totalFor(d.getTypeDemandeEnum(), d.getDureeMesse(), d.getDates());
        d.setPrixTotal(total);

        Demande saved = demandeRepository.save(d);
        return demandeMapper.modelToDto(saved);
    }

    @Override
    public DemandeResponse validateDemande(UUID publicId) {
        if (publicId == null) throw new IllegalArgumentException("publicId manquant");
        Demande d = demandeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        if (Boolean.TRUE.equals(d.getStatusDel()))
            throw new IllegalStateException("Demande supprimée");

        d.setStatusValidationEnum(StatusValidationEnum.VALIDEE);
        return demandeMapper.modelToDto(demandeRepository.save(d));
    }

    @Override
    public DemandeResponse cancelDemande(UUID publicId) {
        if (publicId == null) throw new IllegalArgumentException("publicId manquant");
        Demande d = demandeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        if (Boolean.TRUE.equals(d.getStatusDel()))
            throw new IllegalStateException("Demande supprimée");

        d.setStatusValidationEnum(StatusValidationEnum.ANNULEE);
        return demandeMapper.modelToDto(demandeRepository.save(d));
    }

    @Override
    public void delete(UUID publicId) {
        if (publicId == null) throw new IllegalArgumentException("publicId manquant");
        Demande d = demandeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        d.setStatusDel(true); // soft delete
        demandeRepository.save(d);
    }
}
