package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.mappers.UserMapper;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("User response contract tests")
class UserResponseContractTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParoisseRepository paroisseRepository;

    @Mock
    private ParoisseAccessRepository paroisseAccessRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Legacy mapper exposes active and global status")
    void legacyMapperExposesStatusFields() {
        User user = user(false, true, UserRole.ADMIN, "local.admin");
        UserMapper mapper = Mappers.getMapper(UserMapper.class);

        UserResponse response = mapper.toResponse(user);

        assertFalse(response.isActive());
        assertTrue(response.isGlobal());
        assertEquals("ADMIN", response.role());
    }

    @Test
    @DisplayName("Canonical user service exposes active and global status")
    void canonicalServiceExposesStatusFields() {
        UUID actorId = UUID.randomUUID();
        User actor = user(true, true, UserRole.SUPER_ADMIN, "root.admin");
        User savedUser = user(false, true, UserRole.ADMIN, "inactive.global");
        UserRequest request = new UserRequest(
                "Koffi",
                "Test",
                "inactive.global",
                "SecurePassword123!",
                true,
                false,
                UserRole.ADMIN,
                null
        );

        when(userRepository.findByPublicIdAndStatusDelFalse(actorId)).thenReturn(Optional.of(actor));
        when(userRepository.existsByUsernameAndStatusDelFalse("inactive.global")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        EnhancedUserService service = new EnhancedUserService(
                userRepository,
                paroisseRepository,
                paroisseAccessRepository,
                passwordEncoder
        );

        UserResponse response = service.createUser(request, actorId);

        assertFalse(response.isActive());
        assertTrue(response.isGlobal());
        assertEquals("inactive.global", response.username());
    }

    private User user(boolean active, boolean global, UserRole role, String username) {
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setNom("Koffi");
        user.setPrenom("Test");
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setIsActive(active);
        user.setIsGlobal(global);
        user.setStatusDel(false);
        return user;
    }
}
