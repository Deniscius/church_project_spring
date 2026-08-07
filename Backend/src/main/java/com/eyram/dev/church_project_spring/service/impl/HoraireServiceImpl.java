package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.HoraireRequest;
import com.eyram.dev.church_project_spring.DTO.response.HoraireResponse;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseHorairesPublicResponse;
import com.eyram.dev.church_project_spring.DTO.response.ProgrammeJourResponse;
import com.eyram.dev.church_project_spring.config.CacheConfig;
import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.enums.NatureForfaitEnum;
import com.eyram.dev.church_project_spring.mappers.HoraireMapper;
import com.eyram.dev.church_project_spring.repositories.HoraireRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.HoraireService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HoraireServiceImpl implements HoraireService {

    private static final int MAX_PROGRAMME_DAYS = 62;

    private final HoraireRepository horaireRepository;
    private final ParoisseRepository paroisseRepository;
    private final HoraireMapper horaireMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    @CacheEvict(cacheNames = CacheConfig.HORAIRES_PUBLIC_ACTIVES, allEntries = true)
    public HoraireResponse create(HoraireRequest request) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        tenantAccessService.checkCatalogWriteAccess(paroisse);

        NormalizedSlot slot = normalizeRequest(request, null);
        assertNoConflict(paroisse, slot, request.heureCelebration(), null);

        Horaire horaire = horaireMapper.dtoToModel(request);
        horaire.setParoisse(paroisse);
        horaire.setJourSemaine(slot.jourSemaine());
        horaire.setDateSpecifique(slot.dateSpecifique());
        horaire.setUniqueSurParoisse(slot.uniqueSurParoisse());
        horaire.setNatureHonoraire(slot.natureHonoraire());

        Horaire savedHoraire = horaireRepository.save(horaire);
        return horaireMapper.modelToDto(savedHoraire);
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.HORAIRES_PUBLIC_ACTIVES, allEntries = true)
    public HoraireResponse update(UUID publicId, HoraireRequest request) {
        Horaire existingHoraire = horaireRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable"));

        tenantAccessService.checkCatalogWriteAccess(existingHoraire.getParoisse());

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        tenantAccessService.checkCatalogWriteAccess(paroisse);

        NormalizedSlot slot = normalizeRequest(request, existingHoraire);
        assertNoConflict(paroisse, slot, request.heureCelebration(), publicId);

        horaireMapper.updateEntityFromDto(request, existingHoraire);
        existingHoraire.setParoisse(paroisse);
        existingHoraire.setJourSemaine(slot.jourSemaine());
        existingHoraire.setDateSpecifique(slot.dateSpecifique());
        existingHoraire.setUniqueSurParoisse(slot.uniqueSurParoisse());
        existingHoraire.setNatureHonoraire(slot.natureHonoraire());

        Horaire updatedHoraire = horaireRepository.save(existingHoraire);
        return horaireMapper.modelToDto(updatedHoraire);
    }

    @Override
    @Transactional(readOnly = true)
    public HoraireResponse getByPublicId(UUID publicId) {
        Horaire horaire = horaireRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable"));

        return horaireMapper.modelToDto(horaire);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoraireResponse> getAll() {
        return horaireRepository.findByStatusDelFalse()
                .stream()
                .map(horaireMapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoraireResponse> getByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        return horaireRepository.findByParoisseAndStatusDelFalse(paroisse)
                .stream()
                .map(horaireMapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoraireResponse> getActiveByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        return horaireRepository.findByParoisseAndIsActiveTrueAndStatusDelFalse(paroisse)
                .stream()
                .map(horaireMapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.HORAIRES_PUBLIC_ACTIVES)
    public List<ParoisseHorairesPublicResponse> listPublicHorairesForActiveParishes() {
        List<Horaire> horaires = horaireRepository.findActiveHorairesForActiveParishes();
        Map<UUID, ParoisseHorairesPublicResponse> byParish = new LinkedHashMap<>();

        for (Horaire horaire : horaires) {
            Paroisse paroisse = horaire.getParoisse();
            if (paroisse == null || paroisse.getPublicId() == null) {
                continue;
            }
            JourSemaine jour = horaire.getJourSemaine();
            ParoisseHorairesPublicResponse.Creneau creneau = new ParoisseHorairesPublicResponse.Creneau(
                    jour != null ? jour.name() : null,
                    jour != null ? jour.getLibelle() : null,
                    horaire.getHeureCelebration(),
                    horaire.getLibelle()
            );

            byParish.compute(paroisse.getPublicId(), (id, existing) -> {
                if (existing == null) {
                    String doyenneNom = paroisse.getDoyenne() != null ? paroisse.getDoyenne().getNom() : null;
                    List<ParoisseHorairesPublicResponse.Creneau> slots = new ArrayList<>();
                    slots.add(creneau);
                    return new ParoisseHorairesPublicResponse(
                            paroisse.getPublicId(),
                            paroisse.getNom(),
                            doyenneNom,
                            slots
                    );
                }
                existing.horaires().add(creneau);
                return existing;
            });
        }

        return byParish.values().stream()
                .map(group -> {
                    List<ParoisseHorairesPublicResponse.Creneau> sorted = group.horaires().stream()
                            .sorted(Comparator
                                    .comparingInt((ParoisseHorairesPublicResponse.Creneau c) -> {
                                        if (c.jourSemaine() == null) return 99;
                                        try {
                                            return JourSemaine.valueOf(c.jourSemaine()).getOrdre();
                                        } catch (IllegalArgumentException ex) {
                                            return 99;
                                        }
                                    })
                                    .thenComparing(c -> c.heureCelebration() != null
                                            ? c.heureCelebration()
                                            : LocalTime.MIDNIGHT))
                            .toList();
                    return new ParoisseHorairesPublicResponse(
                            group.paroissePublicId(),
                            group.paroisseNom(),
                            group.doyenneNom(),
                            new ArrayList<>(sorted)
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgrammeJourResponse> getProgramme(
            UUID paroissePublicId,
            LocalDate debut,
            LocalDate fin
    ) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        LocalDate start = debut != null ? debut : LocalDate.now();
        LocalDate end = fin != null ? fin : start.plusDays(13);
        if (end.isBefore(start)) {
            throw new BusinessRuleException("La date de fin doit être postérieure ou égale à la date de début");
        }
        if (start.plusDays(MAX_PROGRAMME_DAYS).isBefore(end)) {
            throw new BusinessRuleException("La période de programme ne peut pas dépasser " + MAX_PROGRAMME_DAYS + " jours");
        }

        List<Horaire> weekly = horaireRepository
                .findByParoisseAndIsActiveTrueAndStatusDelFalseAndDateSpecifiqueIsNull(paroisse);
        List<Horaire> oneOffs = horaireRepository
                .findByParoisseAndIsActiveTrueAndStatusDelFalseAndDateSpecifiqueBetween(paroisse, start, end);

        Map<LocalDate, List<Horaire>> oneOffsByDate = new LinkedHashMap<>();
        for (Horaire h : oneOffs) {
            oneOffsByDate.computeIfAbsent(h.getDateSpecifique(), d -> new ArrayList<>()).add(h);
        }

        List<ProgrammeJourResponse> programme = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            programme.add(buildProgrammeDay(day, weekly, oneOffsByDate.getOrDefault(day, List.of())));
        }
        return programme;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Horaire> resolveHorairesForDate(UUID paroissePublicId, LocalDate date) {
        if (date == null) {
            return List.of();
        }
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        List<Horaire> weekly = horaireRepository
                .findByParoisseAndIsActiveTrueAndStatusDelFalseAndDateSpecifiqueIsNull(paroisse);
        List<Horaire> oneOffs = horaireRepository
                .findByParoisseAndIsActiveTrueAndStatusDelFalseAndDateSpecifiqueBetween(paroisse, date, date);

        ProgrammeJourResponse day = buildProgrammeDay(date, weekly, oneOffs);
        if (day.creneaux().isEmpty()) {
            return List.of();
        }
        Map<UUID, Horaire> byId = new LinkedHashMap<>();
        for (Horaire h : weekly) {
            byId.put(h.getPublicId(), h);
        }
        for (Horaire h : oneOffs) {
            byId.put(h.getPublicId(), h);
        }
        return day.creneaux().stream()
                .map(c -> byId.get(c.horairePublicId()))
                .filter(h -> h != null)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDateSpecifiqueProgramme(UUID paroissePublicId, LocalDate date) {
        if (paroissePublicId == null || date == null) {
            return false;
        }
        return resolveHorairesForDate(paroissePublicId, date).stream()
                .anyMatch(h -> h.getDateSpecifique() != null && date.equals(h.getDateSpecifique()));
    }

    @Override
    @Transactional(readOnly = true)
    public void assertHoraireAllowedOnDate(Horaire horaire, LocalDate date) {
        if (horaire == null || date == null || horaire.getParoisse() == null) {
            return;
        }
        if (horaire.getDateSpecifique() != null && !horaire.getDateSpecifique().equals(date)) {
            throw new BusinessRuleException(
                    "Cet horaire est réservé à la date du " + horaire.getDateSpecifique()
            );
        }
        List<Horaire> allowed = resolveHorairesForDate(horaire.getParoisse().getPublicId(), date);
        boolean ok = allowed.stream().anyMatch(h -> h.getPublicId().equals(horaire.getPublicId()));
        if (!ok) {
            throw new BusinessRuleException(
                    "Cet horaire n'est pas au programme de la paroisse pour le " + date
                            + " (messe unique ou créneau ponctuel)"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertUniqueMassSlot(
            UUID paroissePublicId,
            LocalDate date,
            Horaire selectedHoraire,
            LocalTime heurePersonnalisee
    ) {
        if (paroissePublicId == null || date == null) {
            return;
        }
        List<Horaire> allowed = resolveHorairesForDate(paroissePublicId, date);
        List<Horaire> uniqueOnes = allowed.stream()
                .filter(h -> Boolean.TRUE.equals(h.getUniqueSurParoisse()))
                .toList();
        if (uniqueOnes.isEmpty()) {
            return;
        }
        Horaire unique = uniqueOnes.get(0);
        if (heurePersonnalisee != null) {
            throw new BusinessRuleException(
                    "Messe unique le " + date
                            + " : seule l'heure de cette célébration est autorisée ("
                            + unique.getHeureCelebration()
                            + ")"
            );
        }
        if (selectedHoraire == null
                || !selectedHoraire.getPublicId().equals(unique.getPublicId())) {
            throw new BusinessRuleException(
                    "Messe unique le " + date
                            + " : vous devez retenir l'horaire de "
                            + unique.getHeureCelebration()
            );
        }
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.HORAIRES_PUBLIC_ACTIVES, allEntries = true)
    public void deleteByPublicId(UUID publicId) {
        Horaire horaire = horaireRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable"));

        tenantAccessService.checkCatalogWriteAccess(horaire.getParoisse());

        horaire.setIsActive(false);
        horaire.setStatusDel(true);
        horaireRepository.save(horaire);
    }

    private ProgrammeJourResponse buildProgrammeDay(
            LocalDate date,
            List<Horaire> weekly,
            List<Horaire> oneOffsForDay
    ) {
        JourSemaine jour = JourSemaine.fromDayOfWeek(date.getDayOfWeek());
        List<Horaire> uniqueOnes = oneOffsForDay.stream()
                .filter(h -> Boolean.TRUE.equals(h.getUniqueSurParoisse()))
                .toList();

        List<Horaire> resolved;
        boolean messeUnique;
        if (!uniqueOnes.isEmpty()) {
            messeUnique = true;
            resolved = uniqueOnes;
        } else {
            messeUnique = false;
            List<Horaire> weeklyForDay = weekly.stream()
                    .filter(h -> h.getJourSemaine() == jour)
                    .toList();
            resolved = new ArrayList<>(weeklyForDay.size() + oneOffsForDay.size());
            resolved.addAll(weeklyForDay);
            resolved.addAll(oneOffsForDay);
        }

        List<ProgrammeJourResponse.Creneau> creneaux = resolved.stream()
                .sorted(Comparator.comparing(
                        Horaire::getHeureCelebration,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(h -> new ProgrammeJourResponse.Creneau(
                        h.getPublicId(),
                        h.getHeureCelebration(),
                        h.getLibelle(),
                        h.getDateSpecifique() != null,
                        Boolean.TRUE.equals(h.getUniqueSurParoisse()),
                        h.getNatureHonoraire() != null ? h.getNatureHonoraire().name() : null
                ))
                .toList();

        return new ProgrammeJourResponse(
                date,
                jour.name(),
                jour.getLibelle(),
                messeUnique,
                creneaux
        );
    }

    private NormalizedSlot normalizeRequest(HoraireRequest request, Horaire existing) {
        LocalDate dateSpecifique = request.dateSpecifique();
        boolean unique = Boolean.TRUE.equals(request.uniqueSurParoisse());

        if (unique && dateSpecifique == null) {
            throw new BusinessRuleException(
                    "Une messe unique sur la paroisse nécessite une date précise"
            );
        }

        JourSemaine jourSemaine;
        NatureForfaitEnum natureHonoraire = null;
        if (dateSpecifique != null) {
            JourSemaine fromDate = JourSemaine.fromDayOfWeek(dateSpecifique.getDayOfWeek());
            if (request.jourSemaine() != null && request.jourSemaine() != fromDate) {
                throw new BusinessRuleException(
                        "Le jour de la semaine doit correspondre à la date précise ("
                                + fromDate.getLibelle()
                                + ")"
                );
            }
            jourSemaine = fromDate;
            natureHonoraire = request.natureHonoraire() != null
                    ? request.natureHonoraire()
                    : defaultNatureForDay(fromDate);
        } else if (request.jourSemaine() != null) {
            jourSemaine = request.jourSemaine();
        } else if (existing != null && existing.getJourSemaine() != null) {
            jourSemaine = existing.getJourSemaine();
        } else {
            throw new BusinessRuleException("Le jour de la semaine est obligatoire pour un horaire hebdomadaire");
        }

        return new NormalizedSlot(jourSemaine, dateSpecifique, unique, natureHonoraire);
    }

    private static NatureForfaitEnum defaultNatureForDay(JourSemaine jour) {
        return jour == JourSemaine.DIMANCHE ? NatureForfaitEnum.DOMINICALE : NatureForfaitEnum.NORMALE;
    }

    private void assertNoConflict(
            Paroisse paroisse,
            NormalizedSlot slot,
            LocalTime heure,
            UUID excludePublicId
    ) {
        if (slot.dateSpecifique() != null) {
            boolean hourTaken = excludePublicId == null
                    ? horaireRepository.existsByParoisseAndDateSpecifiqueAndHeureCelebrationAndStatusDelFalse(
                    paroisse, slot.dateSpecifique(), heure)
                    : horaireRepository.existsOtherOneOffSlot(
                    paroisse, slot.dateSpecifique(), heure, excludePublicId);
            if (hourTaken) {
                throw new AlreadyExistException("Cet horaire existe déjà pour cette date sur la paroisse");
            }
            if (slot.uniqueSurParoisse()) {
                boolean uniqueTaken = excludePublicId == null
                        ? horaireRepository.existsByParoisseAndDateSpecifiqueAndUniqueSurParoisseTrueAndStatusDelFalse(
                        paroisse, slot.dateSpecifique())
                        : horaireRepository.existsOtherUniqueOnDate(
                        paroisse, slot.dateSpecifique(), excludePublicId);
                if (uniqueTaken) {
                    throw new AlreadyExistException(
                            "Une messe unique est déjà définie pour cette date sur la paroisse"
                    );
                }
            }
            return;
        }

        boolean weeklyTaken = excludePublicId == null
                ? horaireRepository.existsByJourSemaineAndHeureCelebrationAndParoisseAndDateSpecifiqueIsNullAndStatusDelFalse(
                slot.jourSemaine(), heure, paroisse)
                : horaireRepository.existsOtherWeeklySlot(
                paroisse, slot.jourSemaine(), heure, excludePublicId);
        if (weeklyTaken) {
            throw new AlreadyExistException("Cet horaire existe déjà pour cette paroisse");
        }
    }

    private record NormalizedSlot(
            JourSemaine jourSemaine,
            LocalDate dateSpecifique,
            boolean uniqueSurParoisse,
            NatureForfaitEnum natureHonoraire
    ) {
    }
}
