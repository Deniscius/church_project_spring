package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.ForfaitTarifRequest;
import com.eyram.dev.church_project_spring.DTO.response.ForfaitTarifResponse;
import com.eyram.dev.church_project_spring.entities.ForfaitTarif;
import com.eyram.dev.church_project_spring.enums.JourSemaine;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ForfaitTarifMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "typeDemande", ignore = true)
    @Mapping(target = "joursCelebrationAutorises", ignore = true)
    ForfaitTarif dtoToModel(ForfaitTarifRequest request);

    @Mapping(target = "typeDemandePublicId", source = "typeDemande.publicId")
    @Mapping(target = "typeDemandeLibelle", source = "typeDemande.libelle")
    @Mapping(target = "statutLabel", ignore = true)
    @Mapping(target = "resume", ignore = true)
    @Mapping(target = "disponibiliteLabel", ignore = true)
    ForfaitTarifResponse modelToDto(ForfaitTarif entity);

    default ForfaitTarifResponse modelToDtoWithMeta(ForfaitTarif entity) {
        ForfaitTarifResponse dto = modelToDto(entity);
        return new ForfaitTarifResponse(
                dto.publicId(),
                dto.codeForfait(),
                dto.nomForfait(),
                dto.montantForfait(),
                dto.nombreJour(),
                dto.nombreCelebration(),
                dto.joursCelebrationAutorises(),
                dto.heurePersonnalise(),
                dto.libelle(),
                dto.isActive(),
                dto.typeDemandePublicId(),
                dto.typeDemandeLibelle(),
                dto.statusDel(),
                buildStatutLabel(dto.isActive(), dto.statusDel()),
                buildResume(dto.nomForfait(), dto.montantForfait(), dto.nombreJour(), dto.nombreCelebration()),
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

    default String buildResume(String nomForfait, BigDecimal montantForfait, Integer nombreJour, Integer nombreCelebration) {
        String base = nomForfait != null ? nomForfait : "Forfait";
        String montant = montantForfait != null ? montantForfait.stripTrailingZeros().toPlainString() : "non défini";
        String jours = nombreJour != null ? nombreJour.toString() : "0";
        String celebrations = nombreCelebration != null ? nombreCelebration.toString() : "0";
        return base + " • montant : " + montant + " • jours : " + jours + " • célébrations : " + celebrations;
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
    @Mapping(target = "typeDemande", ignore = true)
    @Mapping(target = "joursCelebrationAutorises", ignore = true)
    void updateEntityFromDto(ForfaitTarifRequest request, @MappingTarget ForfaitTarif entity);
}