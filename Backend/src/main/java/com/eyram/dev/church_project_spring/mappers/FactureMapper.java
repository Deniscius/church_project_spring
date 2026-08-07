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

    @Mapping(target = "dateEmission", source = "createdAt")
    @Mapping(target = "demandePublicId", source = "demande.publicId")
    @Mapping(target = "codeSuivieDemande", source = "demande.codeSuivie")
    @Mapping(target = "nomFidele", source = "demande.nomFidele")
    @Mapping(target = "prenomFidele", source = "demande.prenomFidele")
    @Mapping(target = "telFidele", source = "demande.telFidele")
    @Mapping(target = "emailFidele", source = "demande.emailFidele")
    @Mapping(target = "intention", source = "demande.intention")
    @Mapping(target = "typeDemandeLibelle", source = "demande.typeDemande.libelle")
    @Mapping(target = "forfaitNom", source = "demande.forfaitTarif.nomForfait")
    @Mapping(target = "statutDemande", source = "demande.statutDemande")
    @Mapping(target = "typePaiementPublicId", source = "demande.typePaiement.publicId")
    @Mapping(target = "typePaiementLibelle", source = "demande.typePaiement.libelle")
    @Mapping(target = "modePaiement", source = "demande.typePaiement.mode")
    @Mapping(target = "reglement", ignore = true)
    FactureResponse modelToDto(Facture facture);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "demande", ignore = true)
    void dtoToModel(FactureRequest request, @MappingTarget Facture facture);
}