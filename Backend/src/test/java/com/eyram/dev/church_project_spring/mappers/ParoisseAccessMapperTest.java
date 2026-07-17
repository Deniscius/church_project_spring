package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.response.ParoisseAccessResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import com.eyram.dev.church_project_spring.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ParoisseAccessMapper tests")
class ParoisseAccessMapperTest {

    private final ParoisseAccessMapper mapper = Mappers.getMapper(ParoisseAccessMapper.class);

    @Test
    @DisplayName("API response exposes public identifiers without entity graphs")
    void mapsParishAccessToPublicResponse() {
        UUID accessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID parishId = UUID.randomUUID();

        User user = new User();
        user.setPublicId(userId);
        user.setNom("Koffi");
        user.setPrenom("Eyram");
        user.setUsername("eyram.admin");
        user.setRole(UserRole.ADMIN);
        user.setIsActive(true);
        user.setIsGlobal(false);
        user.setStatusDel(false);

        Paroisse paroisse = new Paroisse();
        paroisse.setPublicId(parishId);
        paroisse.setNom("Paroisse Saint-Paul");
        paroisse.setIsActive(true);
        paroisse.setStatusDel(false);

        ParoisseAccess access = new ParoisseAccess();
        access.setPublicId(accessId);
        access.setUser(user);
        access.setParoisse(paroisse);
        access.setRoleParoisse(RoleParoisse.ADMIN);
        access.setActive(true);
        access.setStatusDel(false);

        ParoisseAccessResponse response = mapper.modelToDto(access);

        assertEquals(accessId, response.publicId());
        assertEquals(userId, response.userPublicId());
        assertEquals("Koffi", response.userNom());
        assertEquals("Eyram", response.userPrenom());
        assertEquals("eyram.admin", response.username());
        assertEquals(parishId, response.paroissePublicId());
        assertEquals("Paroisse Saint-Paul", response.paroisseNom());
        assertEquals(RoleParoisse.ADMIN, response.roleParoisse());
        assertTrue(response.active());
    }
}
