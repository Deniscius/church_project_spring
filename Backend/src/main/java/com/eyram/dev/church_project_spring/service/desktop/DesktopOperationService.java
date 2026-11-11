package com.eyram.dev.church_project_spring.service.desktop;

import com.eyram.dev.church_project_spring.DTO.request.*;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.enums.StatutValidationEnum;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import com.eyram.dev.church_project_spring.repositories.*;
import com.eyram.dev.church_project_spring.security.*;
import com.eyram.dev.church_project_spring.service.*;
import com.eyram.dev.church_project_spring.service.tenant.ParishStorageDirectory;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.fasterxml.jackson.databind.*;
import jakarta.validation.*;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class DesktopOperationService {
    public record Operation(@NotNull UUID operationId, @NotBlank String kind, @NotNull JsonNode payload) {}
    public record Cash(@NotNull UUID demandePublicId, @NotNull @Positive Integer expectedMontant) {}
    public record Mutation(@NotNull UUID demandePublicId, @NotNull LocalDateTime expectedUpdatedAt, String intention, StatutValidationEnum statut) {}
    private final EntityManager entities;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final Validator validator;
    private final DemandeService demandes;
    private final DetailsPaiementService paiements;
    private final ParoisseRepository parishes;
    private final DemandeRepository demandeRepository;
    private final TenantAccessService access;
    private final ParishStorageDirectory storage;
    public DesktopOperationService(JdbcTemplate jdbc, ObjectMapper json, Validator validator,
            DemandeService demandes, DetailsPaiementService paiements, ParoisseRepository parishes,
            DemandeRepository demandeRepository, TenantAccessService access, EntityManager entities, ParishStorageDirectory storage) {
        this.jdbc=jdbc; this.json=json; this.validator=validator; this.demandes=demandes;
        this.paiements=paiements; this.parishes=parishes; this.demandeRepository=demandeRepository; this.access=access; this.entities=entities; this.storage=storage;
    }
    private <T> T validate(T value) {
        var errors=validator.validate(value);
        if (!errors.isEmpty()) throw new ConstraintViolationException(errors);
        return value;
    }
    private void permission(String required) {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> required.equals(a.getAuthority())))
            throw new AccessDeniedException("Permission requise : " + required);
    }
    /** JDBC and JPA share the Spring transaction: claim, mutation and result commit atomically. */
    @Transactional(rollbackFor = Exception.class)
    public JsonNode execute(Operation input) throws Exception {
        validate(input);
        UUID owner=access.getCurrentUser().getPublicId();
        DemandeRequest request=null; Cash cash=null; Mutation mutation=null; Demande target=null; UUID parish;
        if ("CREATE_DEMANDE".equals(input.kind())) {
            permission("demand:edit");
            request=validate(json.treeToValue(input.payload(), DemandeRequest.class));
            if (request.userPublicId()!=null && !owner.equals(request.userPublicId()))
                throw new AccessDeniedException("Auteur différent du compte connecté");
            parish=request.paroissePublicId();
            access.checkParoisseAccess(parishes.findByPublicIdAndStatusDelFalse(parish)
                    .orElseThrow(() -> new IllegalArgumentException("Paroisse introuvable")));
        } else if (Set.of("CASH_PAYMENT","UPDATE_INTENTION","VALIDATE_DEMANDE","DELETE_DEMANDE").contains(input.kind())) {
            permission(switch(input.kind()) {case "CASH_PAYMENT" -> "payment:manage"; case "UPDATE_INTENTION" -> "demand:edit"; case "VALIDATE_DEMANDE" -> "demand:validate"; default -> "demand:delete";});
            UUID id;
            if("CASH_PAYMENT".equals(input.kind())) {cash=validate(json.treeToValue(input.payload(),Cash.class));id=cash.demandePublicId();}
            else {mutation=validate(json.treeToValue(input.payload(),Mutation.class));id=mutation.demandePublicId();
                if("UPDATE_INTENTION".equals(input.kind()))validate(new DemandeIntentionRequest(mutation.intention()));
                if("VALIDATE_DEMANDE".equals(input.kind()))validate(new DemandeValidationRequest(mutation.statut()));
            }
            target=demandeRepository.findByPublicIdWithAssociationsIncludingDeleted(id)
                    .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
            access.checkParoisseAccess(target.getParoisse()); parish=target.getParoisse().getPublicId();
        } else throw new IllegalArgumentException("Opération desktop inconnue");
        storage.requireSharedStorage(parish);
        // Normalize object keys so equivalent JSON produces the same digest.
        Object normalized=json.convertValue(input.payload(), Object.class);
        String canonical=json.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true).writeValueAsString(normalized);
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        jdbc.update("INSERT INTO desktop_operation(owner_id,operation_id,kind,payload_hash,parish_id) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
                owner,input.operationId(),input.kind(),hash,parish);
        var row=jdbc.queryForMap("SELECT kind,payload_hash,parish_id,response FROM desktop_operation WHERE owner_id=? AND operation_id=? FOR UPDATE",owner,input.operationId());
        if (!input.kind().equals(row.get("kind")) || !hash.equals(row.get("payload_hash")) || !parish.equals(row.get("parish_id")))
            throw new BusinessRuleException("Cet identifiant d'opération a déjà été utilisé pour un contenu différent");
        if (row.get("response")!=null) {
            JsonNode retained=json.readTree((String)row.get("response"));
            if(request!=null&&!retained.hasNonNull("localReference")){
                attachTrackingReference(input,owner,retained);
                jdbc.update("UPDATE desktop_operation SET response=? WHERE owner_id=? AND operation_id=?",json.writeValueAsString(retained),owner,input.operationId());
            }
            return retained;
        }
        Object result;
        if (request!=null) result=demandes.create(request);
        else {
            // Refresh after the row lock: another device may have changed it since authorization.
            jdbc.queryForList("SELECT id FROM demande WHERE public_id=? FOR UPDATE",target.getPublicId());
            entities.refresh(target);
            if(Boolean.TRUE.equals(target.getStatusDel()))throw new BusinessRuleException("Cette demande a été supprimée. Actualisez la liste.");
            UUID id=target.getPublicId();
            if(cash!=null) {
                if(target.getMontant().intValueExact()!=cash.expectedMontant())
                    throw new BusinessRuleException("Montant modifié : actualisez avant de confirmer l'encaissement");
                paiements.encaisserCaisse(id);
            } else {
                if(!Objects.equals(target.getUpdatedAt(),mutation.expectedUpdatedAt()))
                    throw new BusinessRuleException("Cette demande a été modifiée sur le serveur. Actualisez et rapprochez les changements.");
                switch(input.kind()) {
                    case "UPDATE_INTENTION" -> demandes.updateIntention(id,new DemandeIntentionRequest(mutation.intention()));
                    case "VALIDATE_DEMANDE" -> demandes.updateValidation(id,new DemandeValidationRequest(mutation.statut()));
                    case "DELETE_DEMANDE" -> demandes.deleteByPublicId(id);
                    default -> throw new IllegalArgumentException("Opération inconnue");
                }
            }
            entities.flush(); entities.refresh(target);
            result=Boolean.TRUE.equals(target.getStatusDel())?Map.of("publicId",id,"deleted",true):demandes.getByPublicId(id);
        }
        JsonNode response=json.valueToTree(result);
        if(request!=null)attachTrackingReference(input,owner,response);
        jdbc.update("UPDATE desktop_operation SET response=? WHERE owner_id=? AND operation_id=?",json.writeValueAsString(response),owner,input.operationId());
        return response;
    }
    private void attachTrackingReference(Operation input,UUID owner,JsonNode response){
            String reference="MSL-"+input.operationId().toString().toUpperCase(Locale.ROOT);UUID demandId=UUID.fromString(response.path("publicId").asText());
            int inserted=jdbc.update("INSERT INTO desktop_tracking_reference(reference,demande_public_id,owner_id,operation_id) VALUES (?,?,?,?) ON CONFLICT DO NOTHING",reference,demandId,owner,input.operationId());
            if(inserted==0&&!demandId.equals(jdbc.queryForObject("SELECT demande_public_id FROM desktop_tracking_reference WHERE reference=?",UUID.class,reference)))throw new BusinessRuleException("La référence locale est déjà utilisée par une autre demande");
            ((com.fasterxml.jackson.databind.node.ObjectNode)response).put("localReference",reference);
    }

}
