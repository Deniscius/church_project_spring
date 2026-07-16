package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.entities.Doyenne;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ParoisseMapperTest {

    private final ParoisseMapper mapper = Mappers.getMapper(ParoisseMapper.class);

    @Test
    void mapsDeaneryAndAuditFieldsToResponse() {
        Doyenne doyenne = doyenne();
        Paroisse paroisse = paroisse(doyenne);
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();
        paroisse.setCreatedAt(createdAt);
        paroisse.setUpdatedAt(updatedAt);

        ParoisseResponse response = mapper.modelToDto(paroisse);

        assertEquals(paroisse.getPublicId(), response.publicId());
        assertEquals(doyenne.getPublicId(), response.doyennePublicId());
        assertEquals("Doyenné de Lomé-Centre", response.doyenneNom());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void updatesOnlyEditableFields() {
        Doyenne doyenne = doyenne();
        Paroisse paroisse = paroisse(doyenne);
        UUID publicId = paroisse.getPublicId();

        mapper.updateEntityFromDto(
                new ParoisseRequest("Nouveau nom", "Nouvelle adresse", null, null, UUID.randomUUID()),
                paroisse
        );

        assertEquals(publicId, paroisse.getPublicId());
        assertEquals(doyenne, paroisse.getDoyenne());
        assertEquals("Nouveau nom", paroisse.getNom());
        assertFalse(paroisse.getStatusDel());
    }

    private Doyenne doyenne() {
        Doyenne doyenne = new Doyenne();
        doyenne.setPublicId(UUID.randomUUID());
        doyenne.setNom("Doyenné de Lomé-Centre");
        return doyenne;
    }

    private Paroisse paroisse(Doyenne doyenne) {
        Paroisse paroisse = new Paroisse();
        paroisse.setPublicId(UUID.randomUUID());
        paroisse.setNom("Saint Jean");
        paroisse.setAdresse("12 rue de la Paix");
        paroisse.setIsActive(true);
        paroisse.setStatusDel(false);
        paroisse.setDoyenne(doyenne);
        return paroisse;
    }
}
