package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.RoleEnum;

public record RoleRequest(
        RoleEnum role,
        String description
) {}
