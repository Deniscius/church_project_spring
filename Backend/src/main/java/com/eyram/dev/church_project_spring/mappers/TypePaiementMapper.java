package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.TypePaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypePaiementResponse;
import com.eyram.dev.church_project_spring.entities.TypePaiement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TypePaiementMapper {

    TypePaiement dtoToModel(TypePaiementRequest request);

    TypePaiementResponse modelToDto(TypePaiement typePaiement);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    void dtoToModel(TypePaiementRequest request, @MappingTarget TypePaiement typePaiement);
}