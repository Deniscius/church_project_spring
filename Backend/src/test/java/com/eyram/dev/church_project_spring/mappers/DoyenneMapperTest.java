package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.DoyenneRequest;
import com.eyram.dev.church_project_spring.DTO.response.DoyenneResponse;
import com.eyram.dev.church_project_spring.entities.Doyenne;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DoyenneMapperTest {

    private final DoyenneMapper mapper = Mappers.getMapper(DoyenneMapper.class);

    @Test
    void mapsEntityToPublicResponse() {
        UUID publicId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();
        Doyenne doyenne = new Doyenne();
        doyenne.setPublicId(publicId);
        doyenne.setNom("Doyenné de Lomé-Centre");
        doyenne.setDescription("Zone pastorale");
        doyenne.setCreatedAt(createdAt);
        doyenne.setUpdatedAt(updatedAt);

        DoyenneResponse response = mapper.modelToDto(doyenne);

        assertEquals(publicId, response.publicId());
        assertEquals("Doyenné de Lomé-Centre", response.nom());
        assertEquals("Zone pastorale", response.description());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void updatesOnlyEditableFields() {
        UUID publicId = UUID.randomUUID();
        Doyenne doyenne = new Doyenne();
        doyenne.setPublicId(publicId);
        doyenne.setNom("Ancien nom");
        doyenne.setDescription("Ancienne description");
        doyenne.setStatusDel(false);

        mapper.updateEntityFromDto(new DoyenneRequest("Doyenné de Lomé-Ouest", "Adidogomé"), doyenne);

        assertEquals(publicId, doyenne.getPublicId());
        assertEquals("Doyenné de Lomé-Ouest", doyenne.getNom());
        assertEquals("Adidogomé", doyenne.getDescription());
        assertFalse(doyenne.getStatusDel());
    }
}
