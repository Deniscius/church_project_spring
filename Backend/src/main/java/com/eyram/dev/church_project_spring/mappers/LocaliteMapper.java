package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.LocaliteRequest;
import com.eyram.dev.church_project_spring.DTO.response.LocaliteResponse;
import com.eyram.dev.church_project_spring.entities.Localite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocaliteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    Localite dtoToModel(LocaliteRequest request);

    LocaliteResponse modelToDto(Localite entity);
}