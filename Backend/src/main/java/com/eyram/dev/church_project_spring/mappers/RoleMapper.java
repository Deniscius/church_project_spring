package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.RoleRequest;
import com.eyram.dev.church_project_spring.DTO.response.RoleResponse;
import com.eyram.dev.church_project_spring.entities.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    Role toEntity(RoleRequest request);

    @Mapping(source = "role", target = "role")
    RoleResponse toResponse(Role role);
}
