package com.eyram.dev.church_project_spring.service;
import com.eyram.dev.church_project_spring.DTO.response.AnnuaireParoisseResponse;
import com.eyram.dev.church_project_spring.DTO.response.ParoissePublicResponse;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.DTO.request.ParoisseCoordonneesRequest;
import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;

import java.util.List;
import java.util.UUID;

public interface ParoisseService {

    ParoisseResponse create(ParoisseRequest request);
    List<ParoisseResponse> getAll();

    List<ParoissePublicResponse> listPublicActives();

    /**
     * Paroisses d'un doyenné encore absentes de la plateforme, proposées au
     * formulaire d'inscription pour qu'une paroisse revendique son entrée
     * d'annuaire au lieu d'en saisir une variante orthographique.
     */
    List<AnnuaireParoisseResponse> getAnnuaireDisponible(UUID doyennePublicId);

    ParoisseResponse getByPublicId(UUID publicId);
    ParoisseResponse update(UUID publicId, ParoisseRequest request);

    /** Mise à jour restreinte aux coordonnées, ouverte à l'administrateur de la paroisse. */
    ParoisseResponse updateCoordonnees(UUID publicId, ParoisseCoordonneesRequest request);

    /**
     * Logo du reçu — réservé aux paroisses déjà approuvées / actives.
     */
    ParoisseResponse updateLogo(UUID publicId, org.springframework.web.multipart.MultipartFile logo);

    ParoisseResponse removeLogo(UUID publicId);

    org.springframework.core.io.Resource loadLogo(UUID publicId);

    String logoContentType(UUID publicId);

    void deleteByPublicId(UUID publicId);
}