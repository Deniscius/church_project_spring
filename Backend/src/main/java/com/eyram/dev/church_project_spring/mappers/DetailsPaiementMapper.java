package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.DetailsPaiementRequest;
import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DetailsPaiementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    DetailsPaiement dtoToEntity(DetailsPaiementRequest detailsPaiementRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    DetailsPaiement entityToDto(DetailsPaiement detailsPaiement);

}
