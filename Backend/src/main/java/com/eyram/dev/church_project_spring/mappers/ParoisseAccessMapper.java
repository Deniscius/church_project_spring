package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAccessRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseAccessResponse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ParoisseAccessMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    ParoisseAccess dtoToModel(ParoisseAccessRequest request);

    @Mapping(target = "userPublicId", source = "user.publicId")
    @Mapping(target = "userNom", source = "user.nom")
    @Mapping(target = "userPrenom", source = "user.prenom")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userTelephone", source = "user.telephone")
    @Mapping(target = "userRole", source = "user.role")
    @Mapping(target = "userActive", source = "user.isActive")
    @Mapping(target = "paroissePublicId", source = "paroisse.publicId")
    @Mapping(target = "paroisseNom", source = "paroisse.nom")
    @Mapping(target = "paroisseEmail", source = "paroisse.email")
    @Mapping(target = "paroisseTelephone", source = "paroisse.telephone")
    @Mapping(target = "doyenneNom", source = "paroisse.doyenne.nom")
    @Mapping(target = "paroisseActive", source = "paroisse.isActive")
    @Mapping(target = "paroisseSubscriptionExpiresAt", source = "paroisse.subscriptionExpiresAt")
    ParoisseAccessResponse modelToDto(ParoisseAccess entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    void updateEntityFromDto(ParoisseAccessRequest request, @MappingTarget ParoisseAccess entity);
}