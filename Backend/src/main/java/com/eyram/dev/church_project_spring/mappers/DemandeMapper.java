package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DemandeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "codeSuivie", ignore = true)
    @Mapping(target = "montant", ignore = true)
    @Mapping(target = "statutPaiement", ignore = true)
    @Mapping(target = "statutValidation", ignore = true)
    @Mapping(target = "statutDemande", ignore = true)
    @Mapping(target = "validateBy", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "typeDemande", ignore = true)
    @Mapping(target = "forfaitTarif", ignore = true)
    @Mapping(target = "horaire", ignore = true)
    @Mapping(target = "user", ignore = true)
    Demande dtoToModel(DemandeRequest request);

    @Mapping(target = "paroissePublicId", source = "paroisse.publicId")
    @Mapping(target = "paroisseNom", source = "paroisse.nom")
    @Mapping(target = "typeDemandePublicId", source = "typeDemande.publicId")
    @Mapping(target = "typeDemandeLibelle", source = "typeDemande.libelle")
    @Mapping(target = "forfaitTarifPublicId", source = "forfaitTarif.publicId")
    @Mapping(target = "forfaitTarifNom", source = "forfaitTarif.nomForfait")
    @Mapping(target = "horairePublicId", source = "horaire.publicId")
    @Mapping(target = "horaireLibelle", source = "horaire.libelle")
    @Mapping(target = "userPublicId", source = "user.publicId")
    @Mapping(target = "username", source = "user.username")
    DemandeResponse modelToDto(Demande entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "codeSuivie", ignore = true)
    @Mapping(target = "montant", ignore = true)
    @Mapping(target = "statutPaiement", ignore = true)
    @Mapping(target = "statutValidation", ignore = true)
    @Mapping(target = "statutDemande", ignore = true)
    @Mapping(target = "validateBy", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "typeDemande", ignore = true)
    @Mapping(target = "forfaitTarif", ignore = true)
    @Mapping(target = "horaire", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntityFromDto(DemandeRequest request, @MappingTarget Demande entity);
}