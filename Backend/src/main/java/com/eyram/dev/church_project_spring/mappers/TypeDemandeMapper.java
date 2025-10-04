package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.TypeDemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypeDemandeResponse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class TypeDemandeMapper {

    public TypeDemande dtoToModel(TypeDemandeRequest request) {
        if (request == null) {
            throw new RequestNotFoundException("Requête non trouvée");
        }

        TypeDemande typeDemande = new TypeDemande();

        typeDemande.setLibelle(request.libelle());

        typeDemande.setDescription(request.description());
        typeDemande.setStatusDel(false);

        return typeDemande;
    }

    public TypeDemandeResponse modelToDto(TypeDemande entity) {
        if (entity == null) {
            return null;
        }

        return new TypeDemandeResponse(
                entity.getPublicId(),
                entity.getLibelle(),
                entity.getDescription(),
                entity.getStatusDel()
        );
    }
}
