package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.FactureRequest;
import com.eyram.dev.church_project_spring.DTO.response.FactureResponse;
import com.eyram.dev.church_project_spring.entities.Facture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FactureMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "demande", ignore = true)
    Facture dtoToModel(FactureRequest request);

    @Mapping(target = "demandePublicId", source = "demande.publicId")
    @Mapping(target = "codeSuivieDemande", source = "demande.codeSuivie")
    @Mapping(target = "nomFidele", source = "demande.nomFidele")
    @Mapping(target = "prenomFidele", source = "demande.prenomFidele")
    FactureResponse modelToDto(Facture facture);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "demande", ignore = true)
    void dtoToModel(FactureRequest request, @MappingTarget Facture facture);
}