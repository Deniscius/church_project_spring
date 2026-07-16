package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.entities.Localite;
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
    void mapsLocalityAndAuditFieldsToResponse() {
        Localite localite = localite();
        Paroisse paroisse = paroisse(localite);
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();
        paroisse.setCreatedAt(createdAt);
        paroisse.setUpdatedAt(updatedAt);

        ParoisseResponse response = mapper.modelToDto(paroisse);

        assertEquals(paroisse.getPublicId(), response.publicId());
        assertEquals(localite.getPublicId(), response.localitePublicId());
        assertEquals("Lomé", response.localiteVille());
        assertEquals("Tokoin", response.localiteQuartier());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void updatesOnlyEditableFields() {
        Localite localite = localite();
        Paroisse paroisse = paroisse(localite);
        UUID publicId = paroisse.getPublicId();

        mapper.updateEntityFromDto(
                new ParoisseRequest("Nouveau nom", "Nouvelle adresse", null, null, UUID.randomUUID()),
                paroisse
        );

        assertEquals(publicId, paroisse.getPublicId());
        assertEquals(localite, paroisse.getLocalite());
        assertEquals("Nouveau nom", paroisse.getNom());
        assertFalse(paroisse.getStatusDel());
    }

    private Localite localite() {
        Localite localite = new Localite();
        localite.setPublicId(UUID.randomUUID());
        localite.setVille("Lomé");
        localite.setQuartier("Tokoin");
        return localite;
    }

    private Paroisse paroisse(Localite localite) {
        Paroisse paroisse = new Paroisse();
        paroisse.setPublicId(UUID.randomUUID());
        paroisse.setNom("Saint Jean");
        paroisse.setAdresse("12 rue de la Paix");
        paroisse.setIsActive(true);
        paroisse.setStatusDel(false);
        paroisse.setLocalite(localite);
        return paroisse;
    }
}
