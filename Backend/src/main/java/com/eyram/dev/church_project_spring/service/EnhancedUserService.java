package com.eyram.dev.church_project_spring.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAssignmentRequest;
import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedUserService {

    private final UserRepository userRepository;
    private final ParoisseRepository paroisseRepository;
    private final ParoisseAccessRepository paroisseAccessRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(UserRequest request, UUID createdBy) {
        request = normalize(request);
        log.info("Creating new user: {}", request.username());

        validateCommonUserRequest(request);
        validatePasswordForCreate(request.password());
        validateTenantAssignmentForCreate(request);

        User requester = findActiveUser(createdBy);
        assertCanCreateRequestedScope(requester, request);
        validateGlobalRoleConsistency(request);

        if (userRepository.existsByUsernameIgnoreCaseAndStatusDelFalse(request.username())) {
            log.warn("Attempt to create user with duplicate username: {}", request.username());
            throw new BusinessRuleException("Le nom d'utilisateur est déjà pris");
        }

        User user = new User();
        user.setNom(request.nom());
        user.setPrenom(request.prenom());
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setIsGlobal(request.isGlobal());
        user.setIsActive(request.isActive());
        user.setStatusDel(false);

        User savedUser = userRepository.save(user);

        if (!Boolean.TRUE.equals(savedUser.getIsGlobal())) {
            assignParoisses(savedUser, request.paroisses());
        }

        log.info("User created successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getId());
        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(UUID publicId, UserRequest request, UUID updatedBy) {
        request = normalize(request);
        log.info("Updating user: {}", publicId);

        User requester = findActiveUser(updatedBy);
        User user = findActiveUser(publicId);
        assertCanAccessUser(requester, user);
        assertTenantScopeUnchanged(user, request);
        assertCanApplyRequestedScope(requester, request);
        validateGlobalRoleConsistency(request);

        validateCommonUserRequest(request);
        validatePasswordForUpdate(request.password());

        if (user.getPublicId().equals(requester.getPublicId())
                && !Boolean.TRUE.equals(request.isActive())) {
            throw new BusinessRuleException("Vous ne pouvez pas désactiver votre propre compte");
        }
        ensureAnotherGlobalSuperAdminExistsBeforeDeactivation(user, request.isActive());

        if (!user.getUsername().equals(request.username())
                && userRepository.existsByUsernameIgnoreCaseAndStatusDelFalse(request.username())) {
            throw new BusinessRuleException("Le nom d'utilisateur est déjà pris");
        }

        user.setNom(request.nom());
        user.setPrenom(request.prenom());
        user.setUsername(request.username());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        user.setRole(request.role());
        user.setIsGlobal(request.isGlobal());
        user.setIsActive(request.isActive());

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", publicId);

        return mapToResponse(updatedUser);
    }

    @Transactional
    public void assignParoisseToUser(
            UUID userPublicId,
            ParoisseAssignmentRequest assignment,
            UUID createdBy
    ) {
        log.info("Assigning paroisse {} to user (ID: {})", assignment.getParoisseId(), userPublicId);

        User requester = findActiveUser(createdBy);
        User user = findActiveUser(userPublicId);
        assertCanAccessUser(requester, user);
        assertCanAssignParoisse(requester, assignment);

        if (Boolean.TRUE.equals(user.getIsGlobal())) {
            throw new BusinessRuleException(
                    "Un utilisateur global ne doit pas être assigné à une paroisse"
            );
        }

        assignParoisse(user, assignment);
    }

    @Transactional
    public void revokeParoisseAccess(UUID userPublicId, Long paroisseId, UUID revokedBy) {
        log.info("Revoking paroisse access for user {} -> internal parish ID {}", userPublicId, paroisseId);

        User requester = findActiveUser(revokedBy);
        User user = findActiveUser(userPublicId);
        assertCanAccessUser(requester, user);
        assertCanAccessParoisse(requester, paroisseId);

        Paroisse paroisse = paroisseRepository.findById(paroisseId)
                .filter(value -> !Boolean.TRUE.equals(value.getStatusDel()))
                .orElseThrow(() -> new EntityNotFoundException("Paroisse non trouvée"));

        revokeAccess(user, paroisse);
    }

    @Transactional
    public void revokeParoisseAccess(UUID userPublicId, UUID paroissePublicId, UUID revokedBy) {
        log.info("Revoking paroisse access for user {} -> parish {}", userPublicId, paroissePublicId);

        User requester = findActiveUser(revokedBy);
        User user = findActiveUser(userPublicId);
        assertCanAccessUser(requester, user);
        assertCanAccessParoisse(requester, paroissePublicId);

        Paroisse paroisse = paroisseRepository
                .findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new EntityNotFoundException("Paroisse non trouvée"));

        revokeAccess(user, paroisse);
    }

    /**
     * Accès interne sans filtrage par administrateur appelant.
     * Les contrôleurs doivent utiliser la surcharge avec requestedBy.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByParoisse(Long paroisseId) {
        log.debug("Fetching users for internal paroisse ID: {}", paroisseId);

        Paroisse paroisse = paroisseRepository.findById(paroisseId)
                .filter(value -> !Boolean.TRUE.equals(value.getStatusDel()))
                .orElseThrow(() -> new EntityNotFoundException("Paroisse non trouvée"));

        return getActiveUsersForParoisse(paroisse);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByParoisse(Long paroisseId, UUID requestedBy) {
        User requester = findActiveUser(requestedBy);
        assertCanAccessParoisse(requester, paroisseId);
        return getUsersByParoisse(paroisseId);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByParoisse(UUID paroissePublicId) {
        log.debug("Fetching users for parish: {}", paroissePublicId);

        Paroisse paroisse = paroisseRepository
                .findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new EntityNotFoundException("Paroisse non trouvée"));

        return getActiveUsersForParoisse(paroisse);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByParoisse(UUID paroissePublicId, UUID requestedBy) {
        User requester = findActiveUser(requestedBy);
        assertCanAccessParoisse(requester, paroissePublicId);
        return getUsersByParoisse(paroissePublicId);
    }

    /**
     * Accès interne sans filtrage par administrateur appelant.
     * Les contrôleurs doivent utiliser la surcharge avec requestedBy.
     */
    @Transactional(readOnly = true)
    public List<ParoisseAccess> getUserParoisses(UUID userPublicId) {
        User user = findActiveUser(userPublicId);
        return paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(user);
    }

    @Transactional(readOnly = true)
    public List<ParoisseAccess> getUserParoisses(UUID userPublicId, UUID requestedBy) {
        User requester = findActiveUser(requestedBy);
        User target = findActiveUser(userPublicId);
        assertCanAccessUser(requester, target);
        return paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(target);
    }

    /**
     * Accès interne sans filtrage par administrateur appelant.
     * Les contrôleurs doivent utiliser la surcharge avec requestedBy.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByPublicId(UUID userPublicId) {
        return mapToResponse(findActiveUser(userPublicId));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByPublicId(UUID userPublicId, UUID requestedBy) {
        User requester = findActiveUser(requestedBy);
        User target = findActiveUser(userPublicId);
        assertCanAccessUser(requester, target);
        return mapToResponse(target);
    }

    /**
     * Accès interne sans filtrage par administrateur appelant.
     * Les contrôleurs doivent utiliser la surcharge avec requestedBy.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllActiveUsers() {
        log.debug("Fetching all active users");
        return userRepository.findByStatusDelFalseOrderByNomAscPrenomAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllActiveUsers(UUID requestedBy) {
        User requester = findActiveUser(requestedBy);

        if (Boolean.TRUE.equals(requester.getIsGlobal())) {
            return getAllActiveUsers();
        }

        Long paroisseId = resolveSingleActiveParoisseId(requester);
        return getUsersByParoisse(paroisseId);
    }

    @Transactional
    public void deleteUser(UUID userPublicId, UUID deletedBy) {
        User requester = findActiveUser(deletedBy);
        User user = findActiveUser(userPublicId);
        assertCanAccessUser(requester, user);

        if (user.getPublicId().equals(requester.getPublicId())) {
            throw new BusinessRuleException("Vous ne pouvez pas supprimer votre propre compte");
        }
        ensureAnotherGlobalSuperAdminExistsBeforeDeactivation(user, false);

        paroisseAccessRepository.findByUserAndStatusDelFalse(user)
                .forEach(access -> {
                    access.setActive(false);
                    access.setStatusDel(true);
                });

        user.setIsActive(false);
        user.setStatusDel(true);
        userRepository.save(user);

        log.info(
                "User deleted successfully (soft delete): {} (deleted by: {})",
                user.getUsername(),
                deletedBy
        );
    }

    private User findActiveUser(UUID publicId) {
        if (publicId == null) {
            throw new IllegalArgumentException("L'identifiant de l'utilisateur est obligatoire");
        }
        return userRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
    }

    private void revokeAccess(User user, Paroisse paroisse) {
        ParoisseAccess access = paroisseAccessRepository
                .findByUserAndParoisseAndStatusDelFalse(user, paroisse)
                .orElseThrow(() -> new EntityNotFoundException("Accès à la paroisse non trouvé"));

        boolean revokingLastActiveAccess = Boolean.TRUE.equals(access.getActive())
                && paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(user)
                .stream()
                .allMatch(activeAccess -> activeAccess == access
                        || Objects.equals(activeAccess.getPublicId(), access.getPublicId()));

        access.setActive(false);
        access.setStatusDel(true);
        paroisseAccessRepository.save(access);

        if (revokingLastActiveAccess && Boolean.TRUE.equals(user.getIsActive())) {
            user.setIsActive(false);
            userRepository.save(user);
        }

        log.info("Paroisse access revoked successfully");
    }

    private List<UserResponse> getActiveUsersForParoisse(Paroisse paroisse) {
        return paroisseAccessRepository.findByParoisseAndStatusDelFalse(paroisse)
                .stream()
                .filter(access -> Boolean.TRUE.equals(access.getActive()))
                .map(ParoisseAccess::getUser)
                .filter(user -> user != null && !Boolean.TRUE.equals(user.getStatusDel()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void assertCanAccessParoisse(User requester, Long paroisseId) {
        if (Boolean.TRUE.equals(requester.getIsGlobal())) {
            return;
        }

        Long requesterParoisseId = resolveSingleActiveParoisseId(requester);
        if (!requesterParoisseId.equals(paroisseId)) {
            throw new AccessDeniedException("Accès interdit aux utilisateurs de cette paroisse");
        }
    }

    private void assertCanAccessParoisse(User requester, UUID paroissePublicId) {
        if (Boolean.TRUE.equals(requester.getIsGlobal())) {
            return;
        }

        UUID requesterParoissePublicId = resolveSingleActiveParoisse(requester).getPublicId();
        if (requesterParoissePublicId == null || !requesterParoissePublicId.equals(paroissePublicId)) {
            throw new AccessDeniedException("Accès interdit aux utilisateurs de cette paroisse");
        }
    }

    private void assertCanAccessUser(User requester, User target) {
        if (Boolean.TRUE.equals(requester.getIsGlobal())) {
            return;
        }

        if (Boolean.TRUE.equals(target.getIsGlobal())) {
            throw new AccessDeniedException("Accès interdit à cet utilisateur");
        }

        Long requesterParoisseId = resolveSingleActiveParoisseId(requester);
        Long targetParoisseId = resolveSingleActiveParoisseId(target);

        if (!requesterParoisseId.equals(targetParoisseId)) {
            throw new AccessDeniedException("Accès interdit à cet utilisateur");
        }
    }

    private void assertTenantScopeUnchanged(User user, UserRequest request) {
        boolean currentGlobal = Boolean.TRUE.equals(user.getIsGlobal());
        boolean requestedGlobal = Boolean.TRUE.equals(request.isGlobal());

        if (currentGlobal != requestedGlobal) {
            throw new BusinessRuleException(
                    "Le périmètre global/local d'un utilisateur ne peut pas être modifié depuis cette opération"
            );
        }
    }

    private void assertCanCreateRequestedScope(User requester, UserRequest request) {
        assertCanApplyRequestedScope(requester, request);

        if (!Boolean.TRUE.equals(request.isGlobal())) {
            assertCanAssignParoisse(requester, request.paroisses().get(0));
        }
    }

    private void assertCanApplyRequestedScope(User requester, UserRequest request) {
        if (Boolean.TRUE.equals(requester.getIsGlobal())) {
            return;
        }

        if (Boolean.TRUE.equals(request.isGlobal()) || request.role() == UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException(
                    "Un administrateur local ne peut pas accorder des privilèges globaux"
            );
        }
    }

    private void assertCanAssignParoisse(
            User requester,
            ParoisseAssignmentRequest assignment
    ) {
        if (Boolean.TRUE.equals(requester.getIsGlobal())) {
            return;
        }

        if (assignment == null || assignment.getParoisseId() == null) {
            throw new BusinessRuleException("La paroisse est obligatoire");
        }

        Paroisse requesterParoisse = resolveSingleActiveParoisse(requester);
        if (!assignment.getParoisseId().equals(requesterParoisse.getPublicId())) {
            throw new AccessDeniedException(
                    "Un administrateur local ne peut attribuer que sa propre paroisse"
            );
        }
    }

    private Paroisse resolveSingleActiveParoisse(User user) {
        List<ParoisseAccess> accesses = paroisseAccessRepository
                .findByUserAndActiveTrueAndStatusDelFalse(user);

        List<Paroisse> paroisses = accesses.stream()
                .map(ParoisseAccess::getParoisse)
                .filter(paroisse -> paroisse != null && paroisse.getId() != null)
                .distinct()
                .toList();

        if (paroisses.size() != 1) {
            throw new AccessDeniedException(
                    "Le compte administrateur doit être associé à une seule paroisse active"
            );
        }

        return paroisses.get(0);
    }

    private Long resolveSingleActiveParoisseId(User user) {
        return resolveSingleActiveParoisse(user).getId();
    }

    private void validateCommonUserRequest(UserRequest request) {
        if (request.nom() == null || request.nom().isBlank()) {
            throw new BusinessRuleException("Le nom est obligatoire");
        }
        if (request.prenom() == null || request.prenom().isBlank()) {
            throw new BusinessRuleException("Le prénom est obligatoire");
        }
        if (request.username() == null || request.username().isBlank()) {
            throw new BusinessRuleException("Le nom d'utilisateur est obligatoire");
        }
        if (request.role() == null) {
            throw new BusinessRuleException("Le rôle est obligatoire");
        }
        if (request.isGlobal() == null) {
            throw new BusinessRuleException("Le statut global est obligatoire");
        }
        if (request.isActive() == null) {
            throw new BusinessRuleException("Le statut actif est obligatoire");
        }
    }

    private void validatePasswordForCreate(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessRuleException("Le mot de passe est obligatoire");
        }
        validatePasswordLength(password);
    }

    private void validatePasswordForUpdate(String password) {
        if (password == null || password.isBlank()) {
            return;
        }
        validatePasswordLength(password);
    }

    private void validatePasswordLength(String password) {
        if (password.length() < 8) {
            throw new BusinessRuleException(
                    "Le mot de passe doit contenir au minimum 8 caractères"
            );
        }
        if (password.length() > 200) {
            throw new BusinessRuleException(
                    "Le mot de passe ne doit pas dépasser 200 caractères"
            );
        }
    }

    private void validateTenantAssignmentForCreate(UserRequest request) {
        List<ParoisseAssignmentRequest> assignments = request.paroisses();

        if (Boolean.TRUE.equals(request.isGlobal())) {
            if (assignments != null && !assignments.isEmpty()) {
                throw new BusinessRuleException(
                        "Un utilisateur global ne doit pas être assigné à une paroisse"
                );
            }
            return;
        }

        if (assignments == null || assignments.size() != 1) {
            throw new BusinessRuleException(
                    "Une seule paroisse active est obligatoire pour un utilisateur non global"
            );
        }
    }

    private void assignParoisses(User user, List<ParoisseAssignmentRequest> assignments) {
        assignments.forEach(assignment -> assignParoisse(user, assignment));
    }

    private void assignParoisse(User user, ParoisseAssignmentRequest assignment) {
        if (assignment == null || assignment.getParoisseId() == null) {
            throw new BusinessRuleException("La paroisse est obligatoire");
        }

        Paroisse paroisse = paroisseRepository
                .findByPublicIdAndStatusDelFalse(assignment.getParoisseId())
                .filter(value -> Boolean.TRUE.equals(value.getIsActive()))
                .orElseThrow(() -> new EntityNotFoundException("Paroisse non trouvée"));

        if (paroisseAccessRepository.existsByUserAndParoisseAndStatusDelFalse(user, paroisse)) {
            throw new BusinessRuleException("L'utilisateur a déjà accès à cette paroisse");
        }

        if (!paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(user).isEmpty()) {
            throw new BusinessRuleException(
                    "L'utilisateur possède déjà une paroisse active"
            );
        }

        ParoisseAccess access = new ParoisseAccess();
        access.setUser(user);
        access.setParoisse(paroisse);
        access.setRoleParoisse(parseRoleParoisse(assignment.getRoleParoisse()));
        access.setActive(true);
        access.setStatusDel(false);

        paroisseAccessRepository.save(access);
        log.info(
                "Paroisse access created: user {} -> paroisse {} with role {}",
                user.getPublicId(),
                paroisse.getId(),
                access.getRoleParoisse()
        );
    }

    private RoleParoisse parseRoleParoisse(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Le rôle dans la paroisse est obligatoire");
        }

        try {
            return RoleParoisse.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Le rôle dans la paroisse est invalide");
        }
    }

    private UserRequest normalize(UserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête utilisateur est obligatoire");
        }

        return new UserRequest(
                normalizeRequired(request.nom(), 2, 100,
                        "Le nom est obligatoire",
                        "Le nom doit contenir entre 2 et 100 caractères"),
                normalizeRequired(request.prenom(), 2, 150,
                        "Le prénom est obligatoire",
                        "Le prénom doit contenir entre 2 et 150 caractères"),
                normalizeUsername(request.username()),
                request.password(),
                request.isGlobal(),
                request.isActive(),
                request.role(),
                request.paroisses()
        );
    }

    private String normalizeUsername(String username) {
        String normalized = normalizeRequired(
                username,
                3,
                100,
                "Le nom d'utilisateur est obligatoire",
                "Le nom d'utilisateur doit contenir entre 3 et 100 caractères"
        ).toLowerCase(Locale.ROOT);

        if (!normalized.matches("^[a-z0-9._-]+$")) {
            throw new BusinessRuleException(
                    "Le nom d'utilisateur contient des caractères non autorisés"
            );
        }
        return normalized;
    }

    private String normalizeRequired(
            String value,
            int minLength,
            int maxLength,
            String requiredMessage,
            String lengthMessage
    ) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(requiredMessage);
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            throw new BusinessRuleException(lengthMessage);
        }
        return normalized;
    }

    private void validateGlobalRoleConsistency(UserRequest request) {
        boolean global = Boolean.TRUE.equals(request.isGlobal());
        boolean superAdmin = request.role() == UserRole.SUPER_ADMIN;

        if (global != superAdmin) {
            throw new BusinessRuleException(
                    "Le rôle SUPER_ADMIN et le statut global doivent être définis ensemble"
            );
        }
    }

    private void ensureAnotherGlobalSuperAdminExistsBeforeDeactivation(
            User user,
            Boolean requestedActive
    ) {
        boolean deactivating = !Boolean.TRUE.equals(requestedActive);
        boolean protectedAccount = Boolean.TRUE.equals(user.getIsGlobal())
                && user.getRole() == UserRole.SUPER_ADMIN
                && Boolean.TRUE.equals(user.getIsActive());

        if (deactivating
                && protectedAccount
                && userRepository.countByStatusDelFalseAndIsActiveTrueAndIsGlobalTrueAndRole(
                        UserRole.SUPER_ADMIN
                ) <= 1) {
            throw new BusinessRuleException(
                    "Le dernier SUPER_ADMIN actif ne peut pas être désactivé"
            );
        }
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getPublicId(),
                user.getNom(),
                user.getPrenom(),
                user.getUsername(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getIsActive(),
                user.getIsGlobal()
        );
    }
}
