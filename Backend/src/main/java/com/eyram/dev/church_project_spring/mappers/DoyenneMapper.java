package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.DoyenneRequest;
import com.eyram.dev.church_project_spring.DTO.response.DoyenneResponse;
import com.eyram.dev.church_project_spring.entities.Doyenne;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DoyenneMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "paroisses", ignore = true)
    // Le rang est arbitré par le service : il se déduit des doyennés existants
    // quand la requête ne le précise pas.
    @Mapping(target = "rang", ignore = true)
    Doyenne dtoToModel(DoyenneRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "paroisses", ignore = true)
    @Mapping(target = "rang", ignore = true)
    void updateEntityFromDto(DoyenneRequest request, @MappingTarget Doyenne entity);

    DoyenneResponse modelToDto(Doyenne entity);
}
