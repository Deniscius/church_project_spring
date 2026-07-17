package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
    @DisplayName("Super admin reaches parish request validation")
    void superAdminCanReachParishValidation() throws Exception {
        mockMvc.perform(post("/paroisses").with(user(globalSuperAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deanery creation requires authentication")
    void deaneryCreationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/doyennes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Local admin cannot create a global deanery")
    void localAdminCannotCreateDeanery() throws Exception {
        mockMvc.perform(post("/doyennes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Super admin reaches deanery request validation")
    void superAdminCanReachDeaneryValidation() throws Exception {
        mockMvc.perform(post("/doyennes").with(user(globalSuperAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Local admin cannot create a global payment type")
    void localAdminCannotCreatePaymentType() throws Exception {
        mockMvc.perform(post("/type-paiement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Global super admin reaches payment type request validation")
    void globalSuperAdminCanReachPaymentTypeValidation() throws Exception {
        mockMvc.perform(post("/type-paiement").with(user(globalSuperAdmin()))
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
    @DisplayName("Super admin reaches parish access request validation")
    void superAdminCanReachParishAccessValidation() throws Exception {
        mockMvc.perform(post("/paroisse-access").with(user(globalSuperAdmin()))
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

    @Test
    @DisplayName("A non-global super admin cannot mutate global references")
    void nonGlobalSuperAdminCannotCreateDeanery() throws Exception {
        UserDetailsImpl nonGlobalSuperAdmin = new UserDetailsImpl(
                UUID.randomUUID(),
                "Invalid local super admin",
                "invalid.super.admin",
                1L,
                false,
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")),
                true
        );

        mockMvc.perform(post("/doyennes").with(user(nonGlobalSuperAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private UserDetailsImpl globalSuperAdmin() {
        return new UserDetailsImpl(
                UUID.randomUUID(),
                "Global super admin",
                "global.super.admin",
                null,
                true,
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")),
                true
        );
    }
}
