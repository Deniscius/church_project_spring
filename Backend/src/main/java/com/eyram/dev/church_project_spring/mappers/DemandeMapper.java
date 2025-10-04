package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.entities.Client;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.enums.StatusValidationEnum;
import com.eyram.dev.church_project_spring.repository.ClientRepository;
import com.eyram.dev.church_project_spring.repository.TypeDemandeRepository;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DemandeMapper {
    private final ClientRepository clientRepository;
    private final TypeDemandeRepository typeDemandeRepository;
    private final ClientMapper clientMapper;
    private final TypeDemandeMapper typeDemandeMapper;

    public DemandeMapper(ClientRepository clientRepository,
                         TypeDemandeRepository typeDemandeRepository,
                         ClientMapper clientMapper,
                         TypeDemandeMapper typeDemandeMapper) {
        this.clientRepository = clientRepository;
        this.typeDemandeRepository = typeDemandeRepository;
        this.clientMapper = clientMapper;
        this.typeDemandeMapper = typeDemandeMapper;
    }

    public Demande dtoToModel(DemandeRequest request) {
        if (request == null) {
            throw new RequestNotFoundException("Requête non trouvée");
        }

        Client client = clientRepository.findByPublicId(request.clientPublicId())
                .orElseThrow(() -> new RequestNotFoundException(
                        "Client non trouvé avec l'identifiant public : " + request.clientPublicId()));

        TypeDemande typeDemande = typeDemandeRepository.findByPublicId(request.typeDemandePublicId())
                .orElseThrow(() -> new RequestNotFoundException(
                        "TypeDemande non trouvé avec l'identifiant public : " + request.typeDemandePublicId()));

        Demande demande = new Demande();
        demande.setPublicId(UUID.randomUUID());
        demande.setIntention(request.intention());
        demande.setDateDemande(request.dateDemande());
        demande.setStatusValidationEnum(StatusValidationEnum.EN_ATTENTE);
        demande.setStatusDel(false);
        demande.setClient(client);
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
                clientMapper.modelToDto(entity.getClient()),
                typeDemandeMapper.modelToDto(entity.getTypeDemande())
        );

    }

}
