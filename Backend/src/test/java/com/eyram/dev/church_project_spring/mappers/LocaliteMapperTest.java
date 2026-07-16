package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.LocaliteRequest;
import com.eyram.dev.church_project_spring.DTO.response.LocaliteResponse;
import com.eyram.dev.church_project_spring.entities.Localite;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LocaliteMapperTest {

    private final LocaliteMapper mapper = Mappers.getMapper(LocaliteMapper.class);

    @Test
    void mapsEntityToPublicResponse() {
        UUID publicId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();
        Localite localite = new Localite();
        localite.setPublicId(publicId);
        localite.setVille("Lomé");
        localite.setQuartier("Tokoin");
        localite.setCreatedAt(createdAt);
        localite.setUpdatedAt(updatedAt);

        LocaliteResponse response = mapper.modelToDto(localite);

        assertEquals(publicId, response.publicId());
        assertEquals("Lomé", response.ville());
        assertEquals("Tokoin", response.quartier());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void updatesOnlyEditableFields() {
        UUID publicId = UUID.randomUUID();
        Localite localite = new Localite();
        localite.setPublicId(publicId);
        localite.setVille("Ancienne ville");
        localite.setQuartier("Ancien quartier");
        localite.setStatusDel(false);

        mapper.updateEntityFromDto(new LocaliteRequest("Lomé", "Adidogomé"), localite);

        assertEquals(publicId, localite.getPublicId());
        assertEquals("Lomé", localite.getVille());
        assertEquals("Adidogomé", localite.getQuartier());
        assertFalse(localite.getStatusDel());
    }
}
