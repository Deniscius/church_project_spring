package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ParoisseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "isSystem", ignore = true)
    @Mapping(target = "doyenne", ignore = true)
    @Mapping(target = "subscriptionExpiresAt", ignore = true)
    // Le cycle de vie commercial ne se pilote pas depuis un formulaire de fiche.
    @Mapping(target = "statutTenant", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "logoPath", ignore = true)
    Paroisse dtoToModel(ParoisseRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "isSystem", ignore = true)
    @Mapping(target = "doyenne", ignore = true)
    @Mapping(target = "subscriptionExpiresAt", ignore = true)
    @Mapping(target = "statutTenant", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "logoPath", ignore = true)
    void updateEntityFromDto(ParoisseRequest request, @MappingTarget Paroisse entity);

    @Mapping(target = "doyennePublicId", source = "doyenne.publicId")
    @Mapping(target = "doyenneNom", source = "doyenne.nom")
    @Mapping(target = "logoPresent", expression = "java(entity.getLogoPath() != null && !entity.getLogoPath().isBlank())")
    ParoisseResponse modelToDto(Paroisse entity);
}
