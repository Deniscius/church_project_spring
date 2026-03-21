package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.entities.*;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.mappers.DemandeMapper;
import com.eyram.dev.church_project_spring.repositories.*;
import com.eyram.dev.church_project_spring.service.DemandeService;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import com.eyram.dev.church_project_spring.utils.exception.TrackingIdNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemandeServiceImpl implements DemandeService {

    private final DemandeRepository demandeRepository;
    private final ParoisseRepository paroisseRepository;
    private final TypeDemandeRepository typeDemandeRepository;
    private final ForfaitTarifRepository forfaitTarifRepository;
    private final HoraireRepository horaireRepository;
    private final UserRepository userRepository;
    private final DemandeMapper demandeMapper;
    private final TypePaiementRepository typePaiementRepository;

    @Override
    public DemandeResponse create(DemandeRequest request) {


        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(request.typeDemandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));

        ForfaitTarif forfaitTarif = forfaitTarifRepository.findByPublicIdAndStatusDelFalse(request.forfaitTarifPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Forfait tarif introuvable"));

        if (!forfaitTarif.getTypeDemande().getPublicId().equals(typeDemande.getPublicId())) {
            throw new IllegalArgumentException("Le forfait ne correspond pas au type de demande");
        }

        Horaire horaire = null;
        if (request.horairePublicId() != null) {
            horaire = horaireRepository.findByPublicIdAndStatusDelFalse(request.horairePublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable"));

            if (!horaire.getParoisse().getPublicId().equals(paroisse.getPublicId())) {
                throw new IllegalArgumentException("L'horaire ne correspond pas à la paroisse choisie");
            }
        }

        User user = null;
        if (request.userPublicId() != null) {
            user = userRepository.findByPublicIdAndStatusDelFalse(request.userPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        }

        validateHoraire(request.heurePersonnalisee(), horaire, forfaitTarif);

        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(request.typePaiementPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement introuvable"));

        Demande demande = demandeMapper.dtoToModel(request);
        demande.setParoisse(paroisse);
        demande.setTypeDemande(typeDemande);
        demande.setForfaitTarif(forfaitTarif);
        demande.setHoraire(horaire);
        demande.setUser(user);
        demande.setTypePaiement(typePaiement);
        demande.setMontant(forfaitTarif.getMontantForfait());
        demande.setCodeSuivie(generateTrackingCode());

        Demande savedDemande = demandeRepository.save(demande);
        return demandeMapper.modelToDto(savedDemande);
    }

    @Override
    public DemandeResponse update(UUID publicId, DemandeRequest request) {

        Demande existingDemande = demandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(request.typeDemandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));

        ForfaitTarif forfaitTarif = forfaitTarifRepository.findByPublicIdAndStatusDelFalse(request.forfaitTarifPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Forfait tarif introuvable"));

        if (!forfaitTarif.getTypeDemande().getPublicId().equals(typeDemande.getPublicId())) {
            throw new IllegalArgumentException("Le forfait ne correspond pas au type de demande");
        }

        Horaire horaire = null;
        if (request.horairePublicId() != null) {
            horaire = horaireRepository.findByPublicIdAndStatusDelFalse(request.horairePublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable"));

            if (!horaire.getParoisse().getPublicId().equals(paroisse.getPublicId())) {
                throw new IllegalArgumentException("L'horaire ne correspond pas à la paroisse choisie");
            }
        }

        User user = null;
        if (request.userPublicId() != null) {
            user = userRepository.findByPublicIdAndStatusDelFalse(request.userPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        }

        validateHoraire(request.heurePersonnalisee(), horaire, forfaitTarif);

        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(request.typePaiementPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement introuvable"));

        demandeMapper.updateEntityFromDto(request, existingDemande);
        existingDemande.setParoisse(paroisse);
        existingDemande.setTypeDemande(typeDemande);
        existingDemande.setForfaitTarif(forfaitTarif);
        existingDemande.setHoraire(horaire);
        existingDemande.setUser(user);
        existingDemande.setTypePaiement(typePaiement);
        existingDemande.setMontant(forfaitTarif.getMontantForfait());

        Demande updatedDemande = demandeRepository.save(existingDemande);
        return demandeMapper.modelToDto(updatedDemande);
    }

    @Override
    public DemandeResponse getByPublicId(UUID publicId) {
        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        return demandeMapper.modelToDto(demande);
    }

    @Override
    public DemandeResponse getByCodeSuivie(String codeSuivie) {
        Demande demande = demandeRepository.findByCodeSuivieAndStatusDelFalse(codeSuivie)
                .orElseThrow(() -> new TrackingIdNotFoundException("Code de suivi introuvable"));

        return demandeMapper.modelToDto(demande);
    }

    @Override
    public List<DemandeResponse> getAll() {
        return demandeRepository.findByStatusDelFalse()
                .stream()
                .map(demandeMapper::modelToDto)
                .toList();
    }

    @Override
    public List<DemandeResponse> getByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        return demandeRepository.findByParoisseAndStatusDelFalse(paroisse)
                .stream()
                .map(demandeMapper::modelToDto)
                .toList();
    }

    @Override
    public List<DemandeResponse> getByParoisseAndStatut(UUID paroissePublicId, StatutDemandeEnum statutDemande) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        return demandeRepository.findByParoisseAndStatutDemandeAndStatusDelFalse(paroisse, statutDemande)
                .stream()
                .map(demandeMapper::modelToDto)
                .toList();
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        demande.setStatusDel(true);
        demandeRepository.save(demande);
    }

    private void validateHoraire(LocalTime heurePersonnalisee, Horaire horaire, ForfaitTarif forfaitTarif) {
        if (Boolean.TRUE.equals(forfaitTarif.getHeurePersonnalise())) {
            if (heurePersonnalisee == null && horaire == null) {
                throw new IllegalArgumentException("Une heure personnalisée ou un horaire doit être renseigné");
            }
        } else {
            if (horaire == null) {
                throw new IllegalArgumentException("Un horaire fixe est obligatoire pour ce forfait");
            }
            if (heurePersonnalisee != null) {
                throw new IllegalArgumentException("L'heure personnalisée n'est pas autorisée pour ce forfait");
            }
        }
    }

    private String generateTrackingCode() {
        String code;
        do {
            code = "DEM" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (demandeRepository.existsByCodeSuivieAndStatusDelFalse(code));
        return code;
    }

    @Override
    public List<DemandeResponse> getByTypePaiement(UUID typePaiementPublicId) {
        return demandeRepository.findByTypePaiementPublicIdAndStatusDelFalse(typePaiementPublicId)
                .stream()
                .map(demandeMapper::modelToDto)
                .toList();
    }
}