package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Security endpoint access rules")
class SecurityEndpointAccessTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Health endpoint is accessible without authentication")
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Standard login reaches validation without authentication")
    void standardLoginIsPublic() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Multi-tenant login reaches validation without authentication")
    void multiTenantLoginIsPublic() throws Exception {
        mockMvc.perform(post("/auth/login-multi-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Public parish listing is accessible without authentication")
    void parishListingIsPublic() throws Exception {
        mockMvc.perform(get("/paroisses"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User creation requires authentication")
    void userCreationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Public demand submission reaches validation without authentication")
    void publicDemandSubmissionReachesValidation() throws Exception {
        mockMvc.perform(post("/demandes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Parish creation requires authentication")
    void parishCreationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/paroisses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SECRETAIRE")
    @DisplayName("Secretary cannot create a parish")
    void secretaryCannotCreateParish() throws Exception {
        mockMvc.perform(post("/paroisses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Super admin reaches parish request validation")
    void superAdminCanReachParishValidation() throws Exception {
        mockMvc.perform(post("/paroisses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Locality creation requires authentication")
    void localityCreationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/localites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Local admin cannot create a global locality")
    void localAdminCannotCreateLocality() throws Exception {
        mockMvc.perform(post("/localites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Super admin reaches locality request validation")
    void superAdminCanReachLocalityValidation() throws Exception {
        mockMvc.perform(post("/localites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Local admin cannot mutate raw parish access assignments")
    void localAdminCannotCreateRawParishAccess() throws Exception {
        mockMvc.perform(post("/paroisse-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Super admin reaches parish access request validation")
    void superAdminCanReachParishAccessValidation() throws Exception {
        mockMvc.perform(post("/paroisse-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECRETAIRE")
    @DisplayName("Secretary cannot configure parish schedules")
    void secretaryCannotCreateSchedule() throws Exception {
        mockMvc.perform(post("/horaires")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Local admin reaches schedule request validation")
    void adminCanReachScheduleValidation() throws Exception {
        mockMvc.perform(post("/horaires")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECRETAIRE")
    @DisplayName("Secretary cannot inspect another user's parish assignments")
    void secretaryCannotReadUserParishAssignments() throws Exception {
        mockMvc.perform(get("/admin/users/{userId}/paroisses", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
