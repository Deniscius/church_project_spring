package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.DetailsPaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.DetailsPaiementResponse;
import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DetailsPaiementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "typePaiement", ignore = true)
    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "montantFrais", ignore = true)
    @Mapping(target = "montantFraisAgregeateur", ignore = true)
    @Mapping(target = "montantFraisPlateforme", ignore = true)
    @Mapping(target = "montantCharge", ignore = true)
    @Mapping(target = "montantNet", ignore = true)
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "paymentUrl", ignore = true)
    @Mapping(target = "encaisseurNom", ignore = true)
    @Mapping(target = "encaisseurUserPublicId", ignore = true)
    DetailsPaiement dtoToModel(DetailsPaiementRequest request);

    @Mapping(target = "typePaiementPublicId", source = "typePaiement.publicId")
    @Mapping(target = "typePaiementLibelle", source = "typePaiement.libelle")
    @Mapping(target = "modePaiement", source = "typePaiement.mode")
    @Mapping(target = "facturePublicId", source = "facture.publicId")
    @Mapping(target = "refFacture", source = "facture.refFacture")
    DetailsPaiementResponse modelToDto(DetailsPaiement detailsPaiement);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "typePaiement", ignore = true)
    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "montantFrais", ignore = true)
    @Mapping(target = "montantFraisAgregeateur", ignore = true)
    @Mapping(target = "montantFraisPlateforme", ignore = true)
    @Mapping(target = "montantCharge", ignore = true)
    @Mapping(target = "montantNet", ignore = true)
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "paymentUrl", ignore = true)
    @Mapping(target = "encaisseurNom", ignore = true)
    @Mapping(target = "encaisseurUserPublicId", ignore = true)
    void dtoToModel(DetailsPaiementRequest request, @MappingTarget DetailsPaiement detailsPaiement);
}