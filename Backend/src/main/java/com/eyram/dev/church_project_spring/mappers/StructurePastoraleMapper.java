package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.StructurePastoraleRequest;
import com.eyram.dev.church_project_spring.DTO.response.StructurePastoraleResponse;
import com.eyram.dev.church_project_spring.entities.StructurePastorale;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface StructurePastoraleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "actif", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StructurePastorale dtoToModel(StructurePastoraleRequest request);

    @Mapping(target = "paroissePublicId", source = "paroisse.publicId")
    @Mapping(target = "paroisseNom", source = "paroisse.nom")
    StructurePastoraleResponse modelToDto(StructurePastorale entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "actif", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(StructurePastoraleRequest request, @MappingTarget StructurePastorale entity);
}
