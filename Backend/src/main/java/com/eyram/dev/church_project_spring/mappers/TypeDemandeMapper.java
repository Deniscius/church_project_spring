package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.TypeDemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypeDemandeResponse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TypeDemandeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    TypeDemande toEntity(TypeDemandeRequest request);

    @Mapping(target = "paroissePublicId", source = "paroisse.publicId")
    @Mapping(target = "paroisseNom", source = "paroisse.nom")
    TypeDemandeResponse toResponse(TypeDemande typeDemande);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    void updateEntityFromRequest(TypeDemandeRequest request, @MappingTarget TypeDemande typeDemande);
}