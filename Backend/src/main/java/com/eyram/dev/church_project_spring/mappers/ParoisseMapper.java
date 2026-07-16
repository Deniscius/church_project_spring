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
    @Mapping(target = "localite", ignore = true)
    Paroisse dtoToModel(ParoisseRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "localite", ignore = true)
    void updateEntityFromDto(ParoisseRequest request, @MappingTarget Paroisse entity);

    @Mapping(target = "localitePublicId", source = "localite.publicId")
    @Mapping(target = "localiteVille", source = "localite.ville")
    @Mapping(target = "localiteQuartier", source = "localite.quartier")
    ParoisseResponse modelToDto(Paroisse entity);
}
