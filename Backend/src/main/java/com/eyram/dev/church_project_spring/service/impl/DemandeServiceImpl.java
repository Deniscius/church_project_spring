package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.entities.*;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.enums.StatutValidationEnum;
import com.eyram.dev.church_project_spring.mappers.DemandeMapper;
import com.eyram.dev.church_project_spring.repositories.*;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.DemandeService;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import com.eyram.dev.church_project_spring.utils.exception.TrackingIdNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DemandeServiceImpl implements DemandeService {

    private final DemandeRepository demandeRepository;
    private final ParoisseRepository paroisseRepository;
    private final TypeDemandeRepository typeDemandeRepository;
    private final ForfaitTarifRepository forfaitTarifRepository;
    private final HoraireRepository horaireRepository;
    private final UserRepository userRepository;
    private final DemandeMapper demandeMapper;
    private final TypePaiementRepository typePaiementRepository;
    private final FactureRepository factureRepository;
    private final DetailsPaiementRepository detailsPaiementRepository;
    private final DemandeDateRepository demandeDateRepository;
    private final TenantAccessService tenantAccessService;

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

        validateDates(request, forfaitTarif);

        Demande demande = demandeMapper.dtoToModel(request);
        demande.setParoisse(paroisse);
        demande.setTypeDemande(typeDemande);
        demande.setForfaitTarif(forfaitTarif);
        demande.setHoraire(horaire);
        demande.setUser(user);
        demande.setTypePaiement(typePaiement);
        demande.setMontant(forfaitTarif.getMontantForfait());
        demande.setCodeSuivie(generateTrackingCode());

        applyInitialStatuses(demande, forfaitTarif);

        Demande savedDemande = demandeRepository.save(demande);

        generateDemandeDates(savedDemande, request);

        Facture facture = new Facture();
        facture.setDemande(savedDemande);
        facture.setMontant(savedDemande.getMontant().intValue());
        facture.setStatutPaiement(savedDemande.getStatutPaiement());
        facture.setDatePaiement(null);
        facture.setRefFacture(generateFactureReference());

        factureRepository.save(facture);

        return buildDemandeResponse(savedDemande);
    }

    @Override
    public DemandeResponse update(UUID publicId, DemandeRequest request) {

        Demande existingDemande = demandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        tenantAccessService.checkParoisseAccess(existingDemande.getParoisse());

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        tenantAccessService.checkParoisseAccess(paroisse);

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
        validateDates(request, forfaitTarif);

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

        applyInitialStatuses(existingDemande, forfaitTarif);

        Demande updatedDemande = demandeRepository.save(existingDemande);

        refreshDemandeDates(updatedDemande, request);

        Facture facture = factureRepository.findByDemandePublicIdAndStatusDelFalse(publicId)
                .orElse(null);

        if (facture != null) {
            facture.setMontant(updatedDemande.getMontant().intValue());
            facture.setStatutPaiement(updatedDemande.getStatutPaiement());
            factureRepository.save(facture);
        }

        return buildDemandeResponse(updatedDemande);
    }

    @Override
    @Transactional(readOnly = true)
    public DemandeResponse getByPublicId(UUID publicId) {
        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        tenantAccessService.checkParoisseAccess(demande.getParoisse());

        return buildDemandeResponse(demande);
    }

    @Override
    @Transactional(readOnly = true)
    public DemandeResponse getByCodeSuivie(String codeSuivie) {
        Demande demande = demandeRepository.findByCodeSuivieAndStatusDelFalse(codeSuivie)
                .orElseThrow(() -> new TrackingIdNotFoundException("Code de suivi introuvable"));

        return buildDemandeResponse(demande);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeResponse> getAll() {
        if (tenantAccessService.isGlobalUser()) {
            return demandeRepository.findByStatusDelFalse()
                    .stream()
                    .map(this::buildDemandeResponse)
                    .toList();
        }

        User currentUser = tenantAccessService.getCurrentUser();

        return demandeRepository.findByStatusDelFalse()
                .stream()
                .filter(demande -> tenantAccessService.canAccessParoisse(demande.getParoisse()))
                .map(this::buildDemandeResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeResponse> getByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        tenantAccessService.checkParoisseAccess(paroisse);

        return demandeRepository.findByParoisseAndStatusDelFalse(paroisse)
                .stream()
                .map(this::buildDemandeResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeResponse> getByParoisseAndStatut(UUID paroissePublicId, StatutDemandeEnum statutDemande) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        tenantAccessService.checkParoisseAccess(paroisse);

        return demandeRepository.findByParoisseAndStatutDemandeAndStatusDelFalse(paroisse, statutDemande)
                .stream()
                .map(this::buildDemandeResponse)
                .toList();
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        tenantAccessService.checkParoisseAccess(demande.getParoisse());

        demande.setStatusDel(true);
        demandeRepository.save(demande);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeResponse> getByTypePaiement(UUID typePaiementPublicId) {
        return demandeRepository.findByTypePaiementPublicIdAndStatusDelFalse(typePaiementPublicId)
                .stream()
                .filter(demande -> tenantAccessService.isGlobalUser() || tenantAccessService.canAccessParoisse(demande.getParoisse()))
                .map(this::buildDemandeResponse)
                .toList();
    }

    private void applyInitialStatuses(Demande demande, ForfaitTarif forfaitTarif) {
        boolean isSpecialRequest = forfaitTarif.getNomForfait() != null
                && forfaitTarif.getNomForfait().trim().equalsIgnoreCase("Messe spéciale");

        if (isSpecialRequest) {
            demande.setStatutValidation(StatutValidationEnum.EN_ATTENTE);
            demande.setStatutDemande(StatutDemandeEnum.EN_ATTENTE);
        } else {
            demande.setStatutValidation(StatutValidationEnum.VALIDEE);
            demande.setStatutDemande(StatutDemandeEnum.VALIDEE);
        }

        demande.setStatutPaiement(StatutPaiementEnum.NON_PAYE);
    }

    private void validateDates(DemandeRequest request, ForfaitTarif forfaitTarif) {
        Integer nombreCelebrations = forfaitTarif.getNombreCelebration();

        if (nombreCelebrations == null || nombreCelebrations <= 0) {
            throw new IllegalArgumentException("Le forfait ne définit pas un nombre valide de célébrations");
        }

        if (request.dateDebut() == null) {
            throw new IllegalArgumentException("La date de début est obligatoire");
        }
    }

    private void generateDemandeDates(Demande demande, DemandeRequest request) {
        Integer nombreCelebrations = demande.getForfaitTarif().getNombreCelebration();

        if (nombreCelebrations == null || nombreCelebrations <= 0) {
            return;
        }

        List<DemandeDate> demandeDates = new ArrayList<>();

        for (int i = 1; i <= nombreCelebrations; i++) {
            DemandeDate demandeDate = new DemandeDate();
            demandeDate.setDemande(demande);
            demandeDate.setOrdre(i);
            demandeDate.setDateCelebration(request.dateDebut().plusDays(i - 1));
            demandeDates.add(demandeDate);
        }

        demandeDateRepository.saveAll(demandeDates);
    }

    private void refreshDemandeDates(Demande demande, DemandeRequest request) {
        List<DemandeDate> anciennesDates =
                demandeDateRepository.findByDemande_IdAndStatusDelFalse(demande.getId());

        if (!anciennesDates.isEmpty()) {
            anciennesDates.forEach(item -> item.setStatusDel(true));
            demandeDateRepository.saveAll(anciennesDates);
        }

        generateDemandeDates(demande, request);
    }

    private DemandeResponse buildDemandeResponse(Demande demande) {
        DemandeResponse base = demandeMapper.modelToDto(demande);

        UUID facturePublicId = null;
        String refFacture = null;
        LocalDateTime dateDetailsPaiement = null;
        String idTransaction = null;
        String numero = null;

        Facture facture = factureRepository.findByDemandePublicIdAndStatusDelFalse(demande.getPublicId())
                .orElse(null);

        if (facture != null) {
            facturePublicId = facture.getPublicId();
            refFacture = facture.getRefFacture();

            DetailsPaiement details = detailsPaiementRepository
                    .findByFacturePublicIdAndStatusDelFalse(facture.getPublicId())
                    .orElse(null);

            if (details != null) {
                dateDetailsPaiement = details.getDateDetailsPaiement();
                idTransaction = details.getIdTransaction();
                numero = details.getNumero();
            }
        }

        return new DemandeResponse(
                base.publicId(),
                base.intention(),
                base.codeSuivie(),
                base.nomFidele(),
                base.prenomFidele(),
                base.telFidele(),
                base.emailFidele(),
                base.montant(),
                base.nomCoursier(),
                base.statutPaiement(),
                base.statutValidation(),
                base.validateBy(),
                base.heurePersonnalisee(),
                base.statutDemande(),

                base.paroissePublicId(),
                base.paroisseNom(),

                base.typeDemandePublicId(),
                base.typeDemandeLibelle(),

                base.forfaitTarifPublicId(),
                base.forfaitTarifNom(),

                base.horairePublicId(),
                base.horaireLibelle(),

                base.userPublicId(),
                base.username(),

                base.typePaiementPublicId(),
                base.typePaiementLibelle(),
                base.modePaiement(),

                base.statusDel(),
                base.createdAt(),
                base.updatedAt(),

                facturePublicId,
                refFacture,
                dateDetailsPaiement,
                idTransaction,
                numero
        );
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
            code = "DEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (demandeRepository.existsByCodeSuivieAndStatusDelFalse(code));
        return code;
    }

    private String generateFactureReference() {
        String ref;
        do {
            ref = "FAC" + System.currentTimeMillis();
        } while (factureRepository.findByRefFactureAndStatusDelFalse(ref).isPresent());
        return ref;
    }
}