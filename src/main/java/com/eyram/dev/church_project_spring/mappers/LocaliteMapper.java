package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.LocaliteRequest;
import com.eyram.dev.church_project_spring.DTO.response.LocaliteResponse;
import com.eyram.dev.church_project_spring.entities.Localite;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LocaliteMapper {


    public Localite dtoToModel(LocaliteRequest localiteRequest) {
        if (localiteRequest == null) {
            throw new RequestNotFoundException("Requête non trouvée");
        }

        Localite localite = new Localite();
        localite.setPublicId(UUID.randomUUID());
        localite.setQuartier(localiteRequest.quartier());
        localite.setVille(localiteRequest.ville());
        localite.setStatusDel(false);

        return localite;
    }


    public LocaliteResponse modelToDto(Localite entity) {
        if (entity == null) {
            throw new ResourceNotFoundException("Localité non trouvée");
        }

        return new LocaliteResponse(
                entity.getPublicId(),
                entity.getVille(),
                entity.getQuartier(),
                entity.getStatusDel()
        );
    }
}
