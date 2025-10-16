package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.entities.Fidele;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.enums.StatusValidationEnum;
import com.eyram.dev.church_project_spring.repository.FideleRepository;
import com.eyram.dev.church_project_spring.repository.TypeDemandeRepository;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DemandeMapper {
    private final FideleRepository fideleRepository;
    private final TypeDemandeRepository typeDemandeRepository;
    private final FideleMapper fideleMapper;
    private final TypeDemandeMapper typeDemandeMapper;

    public DemandeMapper(FideleRepository fideleRepository,
                         TypeDemandeRepository typeDemandeRepository,
                         FideleMapper fideleMapper,
                         TypeDemandeMapper typeDemandeMapper) {
        this.fideleRepository = fideleRepository;
        this.typeDemandeRepository = typeDemandeRepository;
        this.fideleMapper = fideleMapper;
        this.typeDemandeMapper = typeDemandeMapper;
    }

    public Demande dtoToModel(DemandeRequest request) {
        if (request == null) {
            throw new RequestNotFoundException("Requête non trouvée");
        }

        Fidele fidele = fideleRepository.findByPublicId(request.clientPublicId())
                .orElseThrow(() -> new RequestNotFoundException(
                        "Fidele non trouvé avec l'identifiant public : " + request.clientPublicId()));

        TypeDemande typeDemande = typeDemandeRepository.findByPublicId(request.typeDemandePublicId())
                .orElseThrow(() -> new RequestNotFoundException(
                        "TypeDemande non trouvé avec l'identifiant public : " + request.typeDemandePublicId()));

        Demande demande = new Demande();
        demande.setPublicId(UUID.randomUUID());
        demande.setIntention(request.intention());
        demande.setDateDemande(request.dateDemande());
        demande.setStatusValidationEnum(StatusValidationEnum.EN_ATTENTE);
        demande.setStatusDel(false);
        demande.setFidele(fidele);
        demande.setTypeDemande(typeDemande);

        return demande;
    }


    public DemandeResponse modelToDto(Demande entity) {
        if (entity == null) {
            throw new RequestNotFoundException("Demande non trouvée");
        }

        return new DemandeResponse(
                entity.getPublicId(),
                entity.getIntention(),
                entity.getDateDemande(),
                entity.getStatusValidationEnum(),
                entity.getStatusDel(),
                fideleMapper.modelToDto(entity.getFidele()),
                typeDemandeMapper.modelToDto(entity.getTypeDemande())
        );

    }

}
