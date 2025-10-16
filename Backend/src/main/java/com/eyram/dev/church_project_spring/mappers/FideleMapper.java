package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.FideleRequest;
import com.eyram.dev.church_project_spring.DTO.response.FideleResponse;
import com.eyram.dev.church_project_spring.entities.Fidele;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class FideleMapper {

    public Fidele dtoToModel(FideleRequest fideleRequest) {
        if (fideleRequest == null) {
            throw new RequestNotFoundException("Requête non trouvée");
        }

        Fidele fidele = new Fidele();
        fidele.setNom(fideleRequest.nom());
        fidele.setPrenoms(fideleRequest.prenoms());
        fidele.setTel(fideleRequest.tel());
        fidele.setStatusDel(false);

        return fidele;
    }

    public FideleResponse modelToDto(Fidele entity) {
        if (entity == null) {
            throw new RequestNotFoundException("Fidele non trouvé");
        }

        return new FideleResponse(
                entity.getPublicId(),
                entity.getNom(),
                entity.getPrenoms(),
                entity.getTel(),
                entity.getStatusDel()
        );
    }
}
