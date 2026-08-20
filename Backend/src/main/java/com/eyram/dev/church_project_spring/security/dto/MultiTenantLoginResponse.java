package com.eyram.dev.church_project_spring.security.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Réponse de connexion multi-tenant.
 * Le champ {@code token} n'est plus exposé au client (cookie HttpOnly).
 */
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MultiTenantLoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Présent uniquement en mémoire serveur avant pose du cookie — jamais sérialisé si null. */
    @JsonProperty("token")
    private String token;

    @JsonProperty("user")
    private UserInfoDto user;

    @JsonProperty("paroisses")
    private List<ParoisseAccessDto> paroisses;

    @JsonProperty("selectedParoisse")
    private ParoisseAccessDto selectedParoisse;

    /** Info utilisateur et droits effectifs de la session. */
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

        /** Permissions métier calculées côté backend à partir du rôle. */
        @JsonProperty("permissions")
        private List<String> permissions;

        @JsonProperty("isGlobal")
        private Boolean isGlobal;
    }

    /** Accès à une paroisse. */
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

        /** Échéance d'abonnement : la paroisse doit pouvoir anticiper la coupure. */
        @JsonProperty("subscriptionExpiresAt")
        private LocalDateTime subscriptionExpiresAt;
    }
}
