package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.AnneePastoraleRequest;
import com.eyram.dev.church_project_spring.DTO.response.AnneePastoraleResponse;
import com.eyram.dev.church_project_spring.entities.AnneePastorale;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface AnneePastoraleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AnneePastorale dtoToModel(AnneePastoraleRequest request);

    @Mapping(target = "paroissePublicId", source = "paroisse.publicId")
    @Mapping(target = "paroisseNom", source = "paroisse.nom")
    AnneePastoraleResponse modelToDto(AnneePastorale entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(AnneePastoraleRequest request, @MappingTarget AnneePastorale entity);
}
