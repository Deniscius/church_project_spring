package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.ForfaitTarifRequest;
import com.eyram.dev.church_project_spring.DTO.response.ForfaitTarifResponse;
import com.eyram.dev.church_project_spring.entities.ForfaitTarif;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ForfaitTarifMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "typeDemande", ignore = true)
    ForfaitTarif dtoToModel(ForfaitTarifRequest request);

    @Mapping(target = "typeDemandePublicId", source = "typeDemande.publicId")
    @Mapping(target = "typeDemandeLibelle", source = "typeDemande.libelle")
    ForfaitTarifResponse modelToDto(ForfaitTarif entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "typeDemande", ignore = true)
    void updateEntityFromDto(ForfaitTarifRequest request, @MappingTarget ForfaitTarif entity);
}