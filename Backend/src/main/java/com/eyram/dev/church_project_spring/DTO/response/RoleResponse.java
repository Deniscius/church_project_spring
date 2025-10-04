package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.RoleEnum;


public record RoleResponse(
        String publicId,
        RoleEnum role,
        String description,
        Boolean statusDel
)
{
}
