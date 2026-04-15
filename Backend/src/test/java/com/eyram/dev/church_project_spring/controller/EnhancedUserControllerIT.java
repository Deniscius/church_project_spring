package com.eyram.dev.church_project_spring.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.enums.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("EnhancedUserController Integration Tests")
class EnhancedUserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UserRequest validUserRequest;

    @BeforeEach
    void setUp() {
        validUserRequest = new UserRequest(
            "Dupont",
            "Jean",
            "jean.dupont.admin",
            "SecurePassword123!",
            false,
            true,
            UserRole.ADMIN,
            null
        );
    }

    @Test
    @DisplayName("Should create user with POST /admin/users")
    @WithMockUser(roles = "ADMIN")
    void testCreateUserEndpoint() throws Exception {
        mockMvc.perform(post("/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nom").value("Dupont"))
            .andExpect(jsonPath("$.username").value("jean.dupont.admin"))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Should get all users with GET /admin/users")
    @WithMockUser(roles = "ADMIN")
    void testGetAllUsersEndpoint() throws Exception {
        mockMvc.perform(get("/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("Should get user by ID with GET /admin/users/{id}")
    @WithMockUser(roles = "ADMIN")
    void testGetUserByIdEndpoint() throws Exception {
        // First create a user
        var createResult = mockMvc.perform(post("/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String response = createResult.getResponse().getContentAsString();
        var created = objectMapper.readTree(response);
        String userId = created.get("publicId").asText();

        // Then retrieve it
        mockMvc.perform(get("/admin/users/" + userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("jean.dupont.admin"));
    }

    @Test
    @DisplayName("Should update user with PUT /admin/users/{id}")
    @WithMockUser(roles = "ADMIN")
    void testUpdateUserEndpoint() throws Exception {
        // First create a user
        var createResult = mockMvc.perform(post("/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String response = createResult.getResponse().getContentAsString();
        var created = objectMapper.readTree(response);
        String userId = created.get("publicId").asText();

        // Update it
        UserRequest updateRequest = new UserRequest(
            "DupontUpdated",
            "Jean",
            "jean.dupont.admin",
            "NewPassword123!",
            false,
            true,
            UserRole.ADMIN,
            null
        );

        mockMvc.perform(put("/admin/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nom").value("DupontUpdated"));
    }

    @Test
    @DisplayName("Should delete user with DELETE /admin/users/{id}")
    @WithMockUser(roles = "ADMIN")
    void testDeleteUserEndpoint() throws Exception {
        // First create a user
        var createResult = mockMvc.perform(post("/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String response = createResult.getResponse().getContentAsString();
        var created = objectMapper.readTree(response);
        String userId = created.get("publicId").asText();

        // Delete it
        mockMvc.perform(delete("/admin/users/" + userId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should forbid access without authentication")
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/admin/users"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should forbid access for SECRETAIRE role")
    @WithMockUser(roles = "SECRETAIRE")
    void testForbiddenAccessForSecretaire() throws Exception {
        mockMvc.perform(post("/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should validate required fields")
    @WithMockUser(roles = "ADMIN")
    void testValidationErrors() throws Exception {
        UserRequest invalidRequest = new UserRequest(
            "",  // Empty nom
            "Jean",
            "jean.dupont",
            "short",  // Too short password
            false,
            true,
            UserRole.ADMIN,
            null
        );

        mockMvc.perform(post("/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }
}
