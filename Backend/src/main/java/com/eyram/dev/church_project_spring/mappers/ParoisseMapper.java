package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ParoisseMapper {
    public Paroisse dtoToModel(ParoisseRequest paroisseRequest) {
        if (paroisseRequest == null) {
            throw new RequestNotFoundException("Request is null");
        }

        Paroisse paroisse = new Paroisse();
        paroisse.setPublicId(UUID.randomUUID());
        paroisse.setNom(paroisseRequest.nom());
        paroisse.setTel(paroisseRequest.tel());
        paroisse.setStatusDel(false);

        return paroisse;
    }

    public ParoisseResponse modelToDto(Paroisse entity) {
        if (entity == null) {
            throw new RequestNotFoundException("Paroisse entity is null");
        }

        return new ParoisseResponse(
                entity.getPublicId(),
                entity.getNom(),
                entity.getTel(),
                entity.getStatusDel()
        );
    }
}
