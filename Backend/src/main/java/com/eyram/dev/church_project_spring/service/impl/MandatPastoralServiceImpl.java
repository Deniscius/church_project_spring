package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.MandatPastoralRequest;
import com.eyram.dev.church_project_spring.DTO.response.MandatPastoralResponse;
import com.eyram.dev.church_project_spring.entities.ActeurPastoral;
import com.eyram.dev.church_project_spring.entities.AnneePastorale;
import com.eyram.dev.church_project_spring.entities.MandatPastoral;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.StructurePastorale;
import com.eyram.dev.church_project_spring.enums.StatutAnneePastorale;
import com.eyram.dev.church_project_spring.mappers.MandatPastoralMapper;
import com.eyram.dev.church_project_spring.repositories.ActeurPastoralRepository;
import com.eyram.dev.church_project_spring.repositories.AnneePastoraleRepository;
import com.eyram.dev.church_project_spring.repositories.MandatPastoralRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.StructurePastoraleRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.MandatPastoralService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MandatPastoralServiceImpl implements MandatPastoralService {

    private final MandatPastoralRepository mandatRepository;
    private final ParoisseRepository paroisseRepository;
    private final AnneePastoraleRepository anneeRepository;
    private final ActeurPastoralRepository acteurRepository;
    private final StructurePastoraleRepository structureRepository;
    private final MandatPastoralMapper mapper;
    private final TenantAccessService tenantAccessService;

    @Override
    public MandatPastoralResponse create(MandatPastoralRequest request) {
        Context context = requireContext(request);
        tenantAccessService.checkParoisseAccess(context.paroisse());
        assertYearEditable(context.annee());

        if (!Boolean.TRUE.equals(context.acteur().getActif())) {
            throw new BusinessRuleException("L'acteur pastoral sélectionné est inactif");
        }
        if (!Boolean.TRUE.equals(context.structure().getActif())) {
            throw new BusinessRuleException("La structure pastorale sélectionnée est inactive");
        }

        String fonction = normalizeRequired(request.fonction());
        LocalDate dateDebut = request.dateDebut() == null
                ? context.annee().getDateDebut()
                : request.dateDebut();
        LocalDate dateFin = request.dateFin() == null
                ? context.annee().getDateFin()
                : request.dateFin();
        validatePeriod(context.annee(), dateDebut, dateFin);
        assertNoDuplicate(context, fonction, dateDebut, dateFin, null);

        MandatPastoral entity = mapper.dtoToModel(request);
        applyContext(entity, context);
        applyValues(entity, request, fonction, dateDebut, dateFin);
        entity.setStatusDel(false);
        return mapper.modelToDto(mandatRepository.saveAndFlush(entity));
    }

    @Override
    public MandatPastoralResponse update(UUID publicId, MandatPastoralRequest request) {
        MandatPastoral entity = requireMandat(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        assertVersion(entity, request.version());
        assertYearEditable(entity.getAnneePastorale());

        Context context = requireContext(request);
        if (!entity.getParoisse().getPublicId().equals(context.paroisse().getPublicId())) {
            throw new BusinessRuleException("Un mandat pastoral ne peut pas changer de paroisse");
        }
        assertYearEditable(context.annee());

        String fonction = normalizeRequired(request.fonction());
        LocalDate dateDebut = request.dateDebut() == null
                ? context.annee().getDateDebut()
                : request.dateDebut();
        LocalDate dateFin = request.dateFin() == null
                ? context.annee().getDateFin()
                : request.dateFin();
        validatePeriod(context.annee(), dateDebut, dateFin);
        assertNoDuplicate(context, fonction, dateDebut, dateFin, publicId);

        mapper.updateEntityFromDto(request, entity);
        applyContext(entity, context);
        applyValues(entity, request, fonction, dateDebut, dateFin);
        return mapper.modelToDto(mandatRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public MandatPastoralResponse getByPublicId(UUID publicId) {
        MandatPastoral entity = requireMandat(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        return mapper.modelToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MandatPastoralResponse> getByAnnee(
            UUID paroissePublicId,
            UUID anneePastoralePublicId
    ) {
        Paroisse paroisse = requireParoisse(paroissePublicId);
        AnneePastorale annee = requireAnnee(anneePastoralePublicId);
        assertSameParish(paroisse, annee.getParoisse(), "L'année pastorale");
        tenantAccessService.checkParoisseAccess(paroisse);
        return mandatRepository
                .findByParoisseAndAnneePastoraleAndStatusDelFalseOrderByStructurePastorale_OrdreAffichageAscOrdreAffichageAsc(
                        paroisse, annee
                )
                .stream()
                .map(mapper::modelToDto)
                .toList();
    }

    @Override
    public void archive(UUID publicId, Long version) {
        MandatPastoral entity = requireMandat(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        assertVersion(entity, version);
        assertYearEditable(entity.getAnneePastorale());
        entity.setStatusDel(true);
        mandatRepository.save(entity);
    }

    private Context requireContext(MandatPastoralRequest request) {
        Paroisse paroisse = requireParoisse(request.paroissePublicId());
        AnneePastorale annee = requireAnnee(request.anneePastoralePublicId());
        ActeurPastoral acteur = acteurRepository
                .findByPublicIdAndStatusDelFalse(request.acteurPastoralPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Acteur pastoral introuvable"));
        StructurePastorale structure = structureRepository
                .findByPublicIdAndStatusDelFalse(request.structurePastoralePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Structure pastorale introuvable"));

        assertSameParish(paroisse, annee.getParoisse(), "L'année pastorale");
        assertSameParish(paroisse, acteur.getParoisse(), "L'acteur pastoral");
        assertSameParish(paroisse, structure.getParoisse(), "La structure pastorale");
        return new Context(paroisse, annee, acteur, structure);
    }

    private void assertSameParish(Paroisse expected, Paroisse actual, String subject) {
        if (!expected.getPublicId().equals(actual.getPublicId())) {
            throw new BusinessRuleException(subject + " n'appartient pas à la paroisse sélectionnée");
        }
    }

    private void assertYearEditable(AnneePastorale annee) {
        if (annee.getStatut() == StatutAnneePastorale.CLOTUREE) {
            throw new BusinessRuleException(
                    "Une année pastorale clôturée conserve son organisation en lecture seule"
            );
        }
    }

    private void validatePeriod(AnneePastorale annee, LocalDate dateDebut, LocalDate dateFin) {
        if (dateFin.isBefore(dateDebut)) {
            throw new BusinessRuleException("La date de fin doit être postérieure ou égale à la date de début");
        }
        if (dateDebut.isBefore(annee.getDateDebut()) || dateFin.isAfter(annee.getDateFin())) {
            throw new BusinessRuleException(
                    "La période du mandat doit être comprise dans l'année pastorale"
            );
        }
    }

    private void assertNoDuplicate(
            Context context,
            String fonction,
            LocalDate dateDebut,
            LocalDate dateFin,
            UUID excludedPublicId
    ) {
        if (mandatRepository.existsOverlappingMandate(
                context.annee(),
                context.acteur(),
                context.structure(),
                fonction,
                dateDebut,
                dateFin,
                excludedPublicId
        )) {
            throw new AlreadyExistException(
                    "Cet acteur possède déjà cette fonction sur la période indiquée"
            );
        }
    }

    private void applyContext(MandatPastoral entity, Context context) {
        entity.setParoisse(context.paroisse());
        entity.setAnneePastorale(context.annee());
        entity.setActeurPastoral(context.acteur());
        entity.setStructurePastorale(context.structure());
    }

    private void applyValues(
            MandatPastoral entity,
            MandatPastoralRequest request,
            String fonction,
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        entity.setFonction(fonction);
        entity.setAttributions(normalizeNullable(request.attributions()));
        entity.setDateDebut(dateDebut);
        entity.setDateFin(dateFin);
        entity.setOrdreAffichage(request.ordreAffichage() == null ? 0 : request.ordreAffichage());
    }

    private MandatPastoral requireMandat(UUID publicId) {
        return mandatRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Mandat pastoral introuvable"));
    }

    private Paroisse requireParoisse(UUID publicId) {
        return paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
    }

    private AnneePastorale requireAnnee(UUID publicId) {
        return anneeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Année pastorale introuvable"));
    }

    private void assertVersion(MandatPastoral entity, Long requestedVersion) {
        if (requestedVersion == null) {
            throw new BusinessRuleException("La version est obligatoire pour cette opération");
        }
        if (!Objects.equals(entity.getVersion(), requestedVersion)) {
            throw new BusinessRuleException(
                    "Ce mandat a été modifié par un autre utilisateur ; rechargez les données"
            );
        }
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("La fonction est obligatoire");
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private record Context(
            Paroisse paroisse,
            AnneePastorale annee,
            ActeurPastoral acteur,
            StructurePastorale structure
    ) {
    }
}
