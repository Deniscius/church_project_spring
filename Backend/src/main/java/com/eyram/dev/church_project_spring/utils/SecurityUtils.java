package com.eyram.dev.church_project_spring.utils;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;

import com.eyram.dev.church_project_spring.security.UserDetailsImpl;

/**
 * Utilitaire pour extraire les informations de sécurité du contexte.
 * Permet d'accéder à l'utilisateur connecté à partir de n'importe quel service/contrôleur.
 * Classe utilitaire statique - utiliser directement SecurityUtils.getCurrentUserPublicId()
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Classe utilitaire - pas d'instantiation
    }

    /**
     * Récupère l'ID public de l'utilisateur connecté.
     *
     * @return UUID de l'utilisateur connecté
     * @throws IllegalStateException si aucun utilisateur n'est authentifié
     */
    public static UUID getCurrentUserPublicId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Aucun utilisateur authentifié");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userDetails.getPublicId();
        }
        
        throw new IllegalStateException("Impossible d'extraire l'ID de l'utilisateur");
    }

    /**
     * Vérifie si un utilisateur est authentifié.
     *
     * @return true si authentifié
     */
    public static boolean isAuthenticated() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
