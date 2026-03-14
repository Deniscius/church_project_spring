package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAccessRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseAccessResponse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ParoisseAccessMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    ParoisseAccess toEntity(ParoisseAccessRequest request);

    @Mapping(target = "userPublicId", source = "user.publicId")
    @Mapping(target = "userNom", source = "user.nom")
    @Mapping(target = "userPrenom", source = "user.prenom")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "paroissePublicId", source = "paroisse.publicId")
    @Mapping(target = "paroisseNom", source = "paroisse.nom")
    ParoisseAccessResponse toResponse(ParoisseAccess paroisseAccess);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    void updateEntityFromRequest(ParoisseAccessRequest request, @MappingTarget ParoisseAccess paroisseAccess);
}