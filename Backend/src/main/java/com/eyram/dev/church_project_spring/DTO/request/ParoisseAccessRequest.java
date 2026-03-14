package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ParoisseAccessRequest(

        @NotNull(message = "L'utilisateur est obligatoire")
        UUID userPublicId,

        @NotNull(message = "La paroisse est obligatoire")
        UUID paroissePublicId,

        @NotNull(message = "Le rôle paroisse est obligatoire")
        RoleParoisse roleParoisse,

        @NotNull(message = "Le statut actif est obligatoire")
        Boolean active

) {
}