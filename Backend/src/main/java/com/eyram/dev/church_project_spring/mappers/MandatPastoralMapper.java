package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.MandatPastoralRequest;
import com.eyram.dev.church_project_spring.DTO.response.MandatPastoralResponse;
import com.eyram.dev.church_project_spring.entities.MandatPastoral;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface MandatPastoralMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "anneePastorale", ignore = true)
    @Mapping(target = "acteurPastoral", ignore = true)
    @Mapping(target = "structurePastorale", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MandatPastoral dtoToModel(MandatPastoralRequest request);

    @Mapping(target = "paroissePublicId", source = "paroisse.publicId")
    @Mapping(target = "anneePastoralePublicId", source = "anneePastorale.publicId")
    @Mapping(target = "anneePastoraleLibelle", source = "anneePastorale.libelle")
    @Mapping(target = "acteurPastoralPublicId", source = "acteurPastoral.publicId")
    @Mapping(target = "acteurNom", source = "acteurPastoral.nom")
    @Mapping(target = "acteurPrenoms", source = "acteurPastoral.prenoms")
    @Mapping(target = "acteurAppellation", source = "acteurPastoral.appellation")
    @Mapping(target = "acteurTelephone", source = "acteurPastoral.telephone")
    @Mapping(target = "acteurEmail", source = "acteurPastoral.email")
    @Mapping(target = "structurePastoralePublicId", source = "structurePastorale.publicId")
    @Mapping(target = "structurePastoraleNom", source = "structurePastorale.nom")
    MandatPastoralResponse modelToDto(MandatPastoral entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "anneePastorale", ignore = true)
    @Mapping(target = "acteurPastoral", ignore = true)
    @Mapping(target = "structurePastorale", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(MandatPastoralRequest request, @MappingTarget MandatPastoral entity);
}
