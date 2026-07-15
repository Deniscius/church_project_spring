package com.eyram.dev.church_project_spring.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAssignmentRequest;
import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnhancedUserService Tests")
class EnhancedUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParoisseRepository paroisseRepository;

    @Mock
    private ParoisseAccessRepository paroisseAccessRepository;

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

        testUser = user(true);
        testUserRequest = request(true, null);
    }

    @Test
    @DisplayName("Should create global user successfully with valid data")
    void testCreateUserSuccess() {
        when(userRepository.existsByUsernameAndStatusDelFalse("jean.dupont")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = userService.createUser(testUserRequest, testCreatedBy);

        assertNotNull(response);
        assertEquals("jean.dupont", response.username());
        assertEquals("Dupont", response.nom());
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("SecurePassword123!");
        verify(paroisseAccessRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create a local user with exactly one parish")
    void testCreateLocalUserWithOneParish() {
        UUID paroissePublicId = UUID.randomUUID();
        Paroisse paroisse = new Paroisse();
        paroisse.setId(10L);
        paroisse.setPublicId(paroissePublicId);

        User localUser = user(false);
        UserRequest localRequest = request(false, List.of(assignment(paroissePublicId)));

        when(userRepository.existsByUsernameAndStatusDelFalse("jean.dupont")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(localUser);
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId))
                .thenReturn(Optional.of(paroisse));
        when(paroisseAccessRepository.existsByUserAndParoisseAndStatusDelFalse(localUser, paroisse))
                .thenReturn(false);
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(localUser))
                .thenReturn(List.of());

        UserResponse response = userService.createUser(localRequest, testCreatedBy);

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
        verify(paroisseAccessRepository).save(any(ParoisseAccess.class));
    }

    @Test
    @DisplayName("Should reject local user creation without a parish")
    void testCreateLocalUserWithoutParish() {
        UserRequest localRequest = request(false, null);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> userService.createUser(localRequest, testCreatedBy)
        );

        assertEquals(
                "Une seule paroisse active est obligatoire pour un utilisateur non global",
                exception.getMessage()
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject local user creation with multiple parishes")
    void testCreateLocalUserWithMultipleParishes() {
        UserRequest localRequest = request(
                false,
                List.of(assignment(UUID.randomUUID()), assignment(UUID.randomUUID()))
        );

        assertThrows(
                BusinessRuleException.class,
                () -> userService.createUser(localRequest, testCreatedBy)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject parish assignments for a global user")
    void testCreateGlobalUserWithParish() {
        UserRequest globalRequest = request(true, List.of(assignment(UUID.randomUUID())));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> userService.createUser(globalRequest, testCreatedBy)
        );

        assertEquals(
                "Un utilisateur global ne doit pas être assigné à une paroisse",
                exception.getMessage()
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate parish assignment failures so creation can roll back")
    void testCreateLocalUserPropagatesAssignmentFailure() {
        UUID missingParishId = UUID.randomUUID();
        User localUser = user(false);
        UserRequest localRequest = request(false, List.of(assignment(missingParishId)));

        when(userRepository.existsByUsernameAndStatusDelFalse("jean.dupont")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(localUser);
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(missingParishId))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> userService.createUser(localRequest, testCreatedBy)
        );

        verify(paroisseAccessRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when username already exists")
    void testCreateUserDuplicateUsername() {
        when(userRepository.existsByUsernameAndStatusDelFalse("jean.dupont")).thenReturn(true);

        assertThrows(
                BusinessRuleException.class,
                () -> userService.createUser(testUserRequest, testCreatedBy)
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when password is too short")
    void testCreateUserInvalidPassword() {
        UserRequest invalidRequest = new UserRequest(
                "Dupont",
                "Jean",
                "jean.dupont",
                "short",
                true,
                true,
                UserRole.ADMIN,
                null
        );

        assertThrows(
                BusinessRuleException.class,
                () -> userService.createUser(invalidRequest, testCreatedBy)
        );
    }

    @Test
    @DisplayName("Update without a password preserves the existing hash")
    void testUpdateWithoutPasswordKeepsExistingPassword() {
        String existingHash = testUser.getPassword();
        UserRequest updateRequest = new UserRequest(
                "Dupont",
                "Jean",
                "jean.dupont",
                "",
                true,
                true,
                UserRole.ADMIN,
                null
        );

        when(userRepository.findByPublicIdAndStatusDelFalse(testUserId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        UserResponse response = userService.updateUser(testUserId, updateRequest, testCreatedBy);

        assertNotNull(response);
        assertEquals(existingHash, testUser.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Update rejects a new password shorter than eight characters")
    void testUpdateRejectsShortPassword() {
        UserRequest updateRequest = new UserRequest(
                "Dupont",
                "Jean",
                "jean.dupont",
                "short",
                true,
                true,
                UserRole.ADMIN,
                null
        );

        when(userRepository.findByPublicIdAndStatusDelFalse(testUserId))
                .thenReturn(Optional.of(testUser));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> userService.updateUser(testUserId, updateRequest, testCreatedBy)
        );

        assertEquals(
                "Le mot de passe doit contenir au minimum 8 caractères",
                exception.getMessage()
        );
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get user by public ID successfully")
    void testGetUserByPublicIdSuccess() {
        when(userRepository.findByPublicIdAndStatusDelFalse(testUserId)).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getUserByPublicId(testUserId);

        assertNotNull(response);
        assertEquals(testUserId, response.publicId());
        assertEquals("jean.dupont", response.username());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user not found")
    void testGetUserByPublicIdNotFound() {
        when(userRepository.findByPublicIdAndStatusDelFalse(testUserId)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> userService.getUserByPublicId(testUserId)
        );
    }

    @Test
    @DisplayName("Should deactivate and soft delete user")
    void testDeleteUserSuccess() {
        when(userRepository.findByPublicIdAndStatusDelFalse(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.deleteUser(testUserId, testCreatedBy);

        verify(userRepository, times(1)).save(any(User.class));
        assertTrue(testUser.getStatusDel());
        assertFalse(testUser.getIsActive());
    }

    @Test
    @DisplayName("Should get all active users")
    void testGetAllActiveUsersSuccess() {
        when(userRepository.findByStatusDelFalse()).thenReturn(List.of(testUser));

        var users = userService.getAllActiveUsers();

        assertNotNull(users);
        assertEquals(1, users.size());
        verify(userRepository, times(1)).findByStatusDelFalse();
    }

    private User user(boolean global) {
        User user = new User();
        user.setId(1L);
        user.setPublicId(testUserId);
        user.setNom("Dupont");
        user.setPrenom("Jean");
        user.setUsername("jean.dupont");
        user.setPassword("hashedPassword123");
        user.setRole(UserRole.ADMIN);
        user.setIsGlobal(global);
        user.setIsActive(true);
        user.setStatusDel(false);
        return user;
    }

    private UserRequest request(boolean global, List<ParoisseAssignmentRequest> assignments) {
        return new UserRequest(
                "Dupont",
                "Jean",
                "jean.dupont",
                "SecurePassword123!",
                global,
                true,
                UserRole.ADMIN,
                assignments
        );
    }

    private ParoisseAssignmentRequest assignment(UUID paroissePublicId) {
        return ParoisseAssignmentRequest.builder()
                .paroisseId(paroissePublicId)
                .roleParoisse("ADMIN")
                .build();
    }
}
