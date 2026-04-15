package com.eyram.dev.church_project_spring.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Réponse de connexion multi-tenant.
 * Inclut les paroisses auxquelles l'utilisateur a accès.
 */
@Getter
@Setter
@Builder
public class MultiTenantLoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("token")
    private String token;

    @JsonProperty("user")
    private UserInfoDto user;

    @JsonProperty("paroisses")
    private List<ParoisseAccessDto> paroisses;

    @JsonProperty("selectedParoisse")
    private ParoisseAccessDto selectedParoisse;

    /**
     * Info utilisateur de la réponse
     */
    @Getter
    @Setter
    @Builder
    public static class UserInfoDto implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("id")
        private UUID publicId;

        @JsonProperty("nom")
        private String nom;

        @JsonProperty("prenom")
        private String prenom;

        @JsonProperty("username")
        private String username;

        @JsonProperty("role")
        private String role;

        @JsonProperty("isGlobal")
        private Boolean isGlobal;
    }

    /**
     * Accès à une paroisse
     */
    @Getter
    @Setter
    @Builder
    public static class ParoisseAccessDto implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("id")
        private UUID paroisseId;

        @JsonProperty("nom")
        private String paroisseNom;

        @JsonProperty("adresse")
        private String adresse;

        @JsonProperty("roleParoisse")
        private String roleParoisse;

        @JsonProperty("active")
        private Boolean active;
    }
}
