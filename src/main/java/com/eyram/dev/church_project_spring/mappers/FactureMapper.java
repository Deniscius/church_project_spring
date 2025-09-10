package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.FactureResquest;
import com.eyram.dev.church_project_spring.entities.Facture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FactureMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    Facture dtoToentity (FactureResquest factureResquest);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    Facture entityToDto (Facture facture);



}
