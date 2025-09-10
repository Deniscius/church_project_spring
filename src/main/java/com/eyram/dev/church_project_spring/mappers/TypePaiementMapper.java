package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.TypePaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypePaiementResponse;
import com.eyram.dev.church_project_spring.entities.TypePaiement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TypePaiementMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    TypePaiement dtoToEntity(TypePaiementRequest request);

    TypePaiementResponse entityToDto(TypePaiement entity);


}
