package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.Fidele;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.enums.StatusValidationEnum;
import com.eyram.dev.church_project_spring.repositories.FideleRepository;
import com.eyram.dev.church_project_spring.repositories.TypeDemandeRepository;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
        if (request == null) throw new RequestNotFoundException("Requête non trouvée");

        Fidele fidele = fideleRepository.findByPublicId(request.fidelePublicId())
                .orElseThrow(() -> new RequestNotFoundException(
                        "Fidèle non trouvé : " + request.fidelePublicId()));
        TypeDemande typeDemande = null;
        if (request.typeDemandePublicId() != null) {
            typeDemande = typeDemandeRepository.findByPublicId(request.typeDemandePublicId())
                    .orElseThrow(() -> new RequestNotFoundException(
                            "TypeDemande non trouvé : " + request.typeDemandePublicId()));
        }

        Demande d = new Demande();
        d.setPublicId(UUID.randomUUID());
        d.setIntention(request.intention());
        d.setDateDemande(request.dateDemande() != null ? request.dateDemande() : LocalDate.now());
        d.setStatusValidationEnum(StatusValidationEnum.EN_ATTENTE);
        d.setStatusDel(false);


        d.setTypeDemandeEnum(request.typeDemandeEnum());
        d.setDureeMesse(request.dureeMesse());
        d.setDates(request.dates());
        d.setFidele(fidele);
        d.setTypeDemande(typeDemande);
        return d;
    }


    public DemandeResponse modelToDto(Demande entity) {
        if (entity == null) throw new RequestNotFoundException("Demande non trouvée");

        return new DemandeResponse(
                entity.getPublicId(),
                entity.getIntention(),
                entity.getDateDemande(),
                entity.getStatusValidationEnum(),
                entity.getStatusDel(),
                entity.getTypeDemandeEnum(),
                entity.getDureeMesse(),
                entity.getDates(),
                entity.getPrixTotal(),
                fideleMapper.modelToDto(entity.getFidele()),
                entity.getTypeDemande() != null ? typeDemandeMapper.modelToDto(entity.getTypeDemande()) : null
        );
    }
}
