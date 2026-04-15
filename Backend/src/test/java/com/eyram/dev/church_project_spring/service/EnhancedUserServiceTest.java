package com.eyram.dev.church_project_spring.service;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnhancedUserService Tests")
class EnhancedUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EnhancedUserService userService;

    private UUID testUserId;
    private UUID testCreatedBy;
    private User testUser;
    private UserRequest testUserRequest;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testCreatedBy = UUID.randomUUID();
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setPublicId(testUserId);
        testUser.setNom("Dupont");
        testUser.setPrenom("Jean");
        testUser.setUsername("jean.dupont");
        testUser.setPassword("hashedPassword123");
        testUser.setRole(UserRole.ADMIN);
        testUser.setIsGlobal(false);
        testUser.setIsActive(true);
        testUser.setStatusDel(false);

        testUserRequest = new UserRequest(
            "Dupont",
            "Jean",
            "jean.dupont",
            "SecurePassword123!",
            false,
            true,
            UserRole.ADMIN,
            null
        );
    }

    @Test
    @DisplayName("Should create user successfully with valid data")
    void testCreateUserSuccess() {
        // Arrange
        when(userRepository.existsByUsernameAndStatusDelFalse("jean.dupont")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse response = userService.createUser(testUserRequest, testCreatedBy);

        // Assert
        assertNotNull(response);
        assertEquals("jean.dupont", response.username());
        assertEquals("Dupont", response.nom());
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("SecurePassword123!");
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when username already exists")
    void testCreateUserDuplicateUsername() {
        // Arrange
        when(userRepository.existsByUsernameAndStatusDelFalse("jean.dupont")).thenReturn(true);

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> {
            userService.createUser(testUserRequest, testCreatedBy);
        });
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when password is too short")
    void testCreateUserInvalidPassword() {
        // Arrange
        UserRequest invalidRequest = new UserRequest(
            "Dupont", "Jean", "jean.dupont", "short", false, true, UserRole.ADMIN, null
        );

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> {
            userService.createUser(invalidRequest, testCreatedBy);
        });
    }

    @Test
    @DisplayName("Should get user by public ID successfully")
    void testGetUserByPublicIdSuccess() {
        // Arrange
        when(userRepository.findByPublicIdAndStatusDelFalse(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        UserResponse response = userService.getUserByPublicId(testUserId);

        // Assert
        assertNotNull(response);
        assertEquals(testUserId, response.publicId());
        assertEquals("jean.dupont", response.username());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user not found")
    void testGetUserByPublicIdNotFound() {
        // Arrange
        when(userRepository.findByPublicIdAndStatusDelFalse(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            userService.getUserByPublicId(testUserId);
        });
    }

    @Test
    @DisplayName("Should delete user with soft delete")
    void testDeleteUserSuccess() {
        // Arrange
        when(userRepository.findByPublicIdAndStatusDelFalse(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.deleteUser(testUserId, testCreatedBy);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
        assertTrue(testUser.getStatusDel());
    }

    @Test
    @DisplayName("Should get all active users")
    void testGetAllActiveUsersSuccess() {
        // Arrange
        when(userRepository.findByStatusDelFalse()).thenReturn(java.util.List.of(testUser));

        // Act
        var users = userService.getAllActiveUsers();

        // Assert
        assertNotNull(users);
        assertEquals(1, users.size());
        verify(userRepository, times(1)).findByStatusDelFalse();
    }
}
