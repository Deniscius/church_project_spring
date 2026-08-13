package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.DemandeIntentionRequest;
import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.request.DemandeValidationRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeParoisseStatsResponse;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.DTO.response.PageResponse;
import com.eyram.dev.church_project_spring.DTO.response.TrackingByPhoneResponse;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;

import java.util.List;
import java.util.UUID;

public interface DemandeService {

    DemandeResponse create(DemandeRequest request);

    DemandeResponse update(UUID publicId, DemandeRequest request);

    DemandeResponse updateValidation(UUID publicId, DemandeValidationRequest request);

    /** Reformule l'intention sans modifier le reste de la demande. */
    DemandeResponse updateIntention(UUID publicId, DemandeIntentionRequest request);

    /**
     * Change le mode de paiement d'une demande encore non payée (accès public par code).
     */
    DemandeResponse updateTypePaiementByCodeSuivie(String codeSuivie, UUID typePaiementPublicId);

    DemandeResponse getByPublicId(UUID publicId);

    DemandeResponse getByCodeSuivie(String codeSuivie);

    /**
     * Recherche publique par téléphone : retourne uniquement les codes de suivi
     * (anti-énumération partielle + rate-limit côté filtre HTTP).
     */
    TrackingByPhoneResponse findTrackingCodesByPhone(String telephone);

    List<DemandeResponse> getAll();

    /**
     * Audit plateforme (COMPTABLE / SUPER_ADMIN) : liste paginée globale.
     * @param includeDeleted inclure les soft-supprimées
     */
    PageResponse<DemandeResponse> getAllPaged(int page, int size, boolean includeDeleted);

    List<DemandeResponse> getByParoisse(UUID paroissePublicId);

    PageResponse<DemandeResponse> getByParoissePaged(UUID paroissePublicId, int page, int size);

    /**
     * @param includeDeleted si true (rôles comptable / admin), inclut les soft-supprimées
     */
    PageResponse<DemandeResponse> getByParoissePaged(
            UUID paroissePublicId,
            int page,
            int size,
            boolean includeDeleted
    );

    DemandeParoisseStatsResponse getParoisseStats(UUID paroissePublicId);

    List<DemandeResponse> getByParoisseAndStatut(UUID paroissePublicId, StatutDemandeEnum statutDemande);

    List<DemandeResponse> getByTypePaiement(UUID typePaiementPublicId);

    void deleteByPublicId(UUID publicId);

    /** Demandes soft-supprimées (trace d'audit) pour une paroisse. */
    List<DemandeResponse> getDeletedByParoisse(UUID paroissePublicId);

    /**
     * Annule les demandes encore non payées dont la première célébration
     * approche (ou est déjà passée). Retourne le nombre d'annulations.
     */
    int cancelUnpaidApproachingCelebrations();

    /**
     * Relance e-mail des demandes non payées dans la fenêtre D-N
     * (toutes les 6 h). Retourne le nombre de rappels envoyés.
     */
    int remindUnpaidApproachingCelebrations();
}
