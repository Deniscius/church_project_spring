package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.HoraireRequest;
import com.eyram.dev.church_project_spring.DTO.request.ProgrammeJourUpdateRequest;
import com.eyram.dev.church_project_spring.DTO.response.HoraireResponse;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseHorairesPublicResponse;
import com.eyram.dev.church_project_spring.DTO.response.ProgrammeJourResponse;
import com.eyram.dev.church_project_spring.entities.Horaire;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface HoraireService {

    HoraireResponse create(HoraireRequest request);

    HoraireResponse update(UUID publicId, HoraireRequest request);

    HoraireResponse getByPublicId(UUID publicId);

    List<HoraireResponse> getAll();

    List<HoraireResponse> getByParoisse(UUID paroissePublicId);

    List<HoraireResponse> getActiveByParoisse(UUID paroissePublicId);

    /** Accueil fidèles : horaires des paroisses à abonnement actif. */
    List<ParoisseHorairesPublicResponse> listPublicHorairesForActiveParishes();

    /**
     * Programme résolu jour par jour : hebdomadaire, ajouts ponctuels,
     * personnalisation complète ou messe unique.
     */
    List<ProgrammeJourResponse> getProgramme(
            UUID paroissePublicId,
            LocalDate debut,
            LocalDate fin
    );

    /**
     * Remplace uniquement le programme disponible d'une date précise.
     * Les demandes déjà enregistrées gardent leur créneau jusqu'à modification explicite.
     */
    ProgrammeJourResponse updateProgrammeForDate(
            UUID paroissePublicId,
            LocalDate date,
            ProgrammeJourUpdateRequest request
    );

    /**
     * Supprime toutes les exceptions de la date et réapplique la grille hebdomadaire.
     */
    ProgrammeJourResponse resetProgrammeForDate(UUID paroissePublicId, LocalDate date);

    /** Créneaux effectivement au programme pour une date donnée. */
    List<Horaire> resolveHorairesForDate(UUID paroissePublicId, LocalDate date);

    /**
     * True si la paroisse a publié au moins un créneau à date précise
     * (événement solennel / ponctuel) pour ce jour au programme.
     */
    boolean isDateSpecifiqueProgramme(UUID paroissePublicId, LocalDate date);

    /**
     * Vérifie qu'un horaire est bien au programme le jour demandé
     * (respecte messe unique / date précise).
     */
    void assertHoraireAllowedOnDate(Horaire horaire, LocalDate date);

    /**
     * Si une messe unique existe pour la date : impose son créneau
     * et interdit toute heure personnalisée.
     */
    void assertUniqueMassSlot(
            UUID paroissePublicId,
            LocalDate date,
            Horaire selectedHoraire,
            LocalTime heurePersonnalisee
    );

    void deleteByPublicId(UUID publicId);
}
