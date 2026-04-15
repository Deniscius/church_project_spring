package com.eyram.dev.church_project_spring.DTO.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Request pour assigner une paroisse à un utilisateur.
 * Utilisé lors de la création ou modification d'accès.
 */
@Getter
@Setter
@Builder
public class ParoisseAssignmentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("paroisseId")
    @NotNull(message = "L'ID de la paroisse est obligatoire")
    private UUID paroisseId;

    @JsonProperty("roleParoisse")
    @NotNull(message = "Le rôle de la paroisse est obligatoire")
    private String roleParoisse;  // ADMIN, GESTIONNAIRE, SECRETAIRE, CONSULTATION
}
