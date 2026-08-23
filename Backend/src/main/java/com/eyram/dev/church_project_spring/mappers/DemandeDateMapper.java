package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.DemandeDateRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeDateResponse;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DemandeDateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "demande", ignore = true)
    @Mapping(target = "horaire", ignore = true)
    @Mapping(target = "heurePersonnalisee", ignore = true)
    @Mapping(target = "celebre", ignore = true)
    @Mapping(target = "celebreAt", ignore = true)
    @Mapping(target = "lastReminderJ1At", ignore = true)
    @Mapping(target = "lastReminderH2At", ignore = true)
    DemandeDate dtoToModel(DemandeDateRequest request);

    @Mapping(target = "demandePublicId", source = "demande.publicId")
    @Mapping(target = "codeSuivieDemande", source = "demande.codeSuivie")
    DemandeDateResponse modelToDto(DemandeDate entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "demande", ignore = true)
    @Mapping(target = "horaire", ignore = true)
    @Mapping(target = "heurePersonnalisee", ignore = true)
    @Mapping(target = "celebre", ignore = true)
    @Mapping(target = "celebreAt", ignore = true)
    @Mapping(target = "lastReminderJ1At", ignore = true)
    @Mapping(target = "lastReminderH2At", ignore = true)
    void updateEntityFromDto(DemandeDateRequest request, @MappingTarget DemandeDate entity);
}