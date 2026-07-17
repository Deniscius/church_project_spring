package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.TypeDemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypeDemandeResponse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.enums.JourSemaine;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface TypeDemandeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "joursCelebrationAutorises", ignore = true)
    TypeDemande dtoToModel(TypeDemandeRequest request);

    @Mapping(target = "paroissePublicId", source = "paroisse.publicId")
    @Mapping(target = "paroisseNom", source = "paroisse.nom")
    @Mapping(target = "statutLabel", ignore = true)
    @Mapping(target = "resume", ignore = true)
    @Mapping(target = "disponibiliteLabel", ignore = true)
    TypeDemandeResponse modelToDto(TypeDemande entity);

    default TypeDemandeResponse modelToDtoWithMeta(TypeDemande entity) {
        TypeDemandeResponse dto = modelToDto(entity);
        return new TypeDemandeResponse(
                dto.publicId(),
                dto.libelle(),
                dto.description(),
                dto.typeDemandeEnum(),
                dto.isActive(),
                dto.delaiMinimumHeures(),
                dto.joursCelebrationAutorises(),
                dto.paroissePublicId(),
                dto.paroisseNom(),
                dto.statusDel(),
                buildStatutLabel(dto.isActive(), dto.statusDel()),
                buildResume(dto.libelle(), dto.typeDemandeEnum(), dto.delaiMinimumHeures()),
                buildDisponibiliteLabel(dto.joursCelebrationAutorises()),
                dto.createdAt(),
                dto.updatedAt()
        );
    }

    default String buildStatutLabel(Boolean isActive, Boolean statusDel) {
        if (Boolean.TRUE.equals(statusDel)) {
            return "Supprimé";
        }
        return Boolean.TRUE.equals(isActive) ? "Actif" : "Inactif";
    }

    default String buildResume(String libelle, Enum<?> typeDemandeEnum, Integer delaiMinimumHeures) {
        String base = libelle != null ? libelle : "Type de demande";
        String typeLabel = typeDemandeEnum != null ? typeDemandeEnum.name() : "Type non défini";
        String delayLabel = delaiMinimumHeures != null ? delaiMinimumHeures + "h" : "non défini";
        return base + " • " + typeLabel + " • délai minimal : " + delayLabel;
    }

    default String buildDisponibiliteLabel(Set<JourSemaine> joursCelebrationAutorises) {
        if (joursCelebrationAutorises == null || joursCelebrationAutorises.isEmpty()) {
            return "Aucun jour configuré";
        }
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (JourSemaine jour : joursCelebrationAutorises) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(jour.name());
            index++;
        }
        return builder.toString();
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "paroisse", ignore = true)
    @Mapping(target = "joursCelebrationAutorises", ignore = true)
    void updateEntityFromDto(TypeDemandeRequest request, @MappingTarget TypeDemande entity);
}