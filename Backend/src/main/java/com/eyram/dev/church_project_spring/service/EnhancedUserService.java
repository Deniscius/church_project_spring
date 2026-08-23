package com.eyram.dev.church_project_spring.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eyram.dev.church_project_spring.DTO.request.ChangePasswordRequest;
import com.eyram.dev.church_project_spring.DTO.request.ParoisseAssignmentRequest;
import com.eyram.dev.church_project_spring.DTO.request.ProfileUpdateRequest;
import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseAccessResponse;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.config.CacheConfig;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.mappers.ParoisseAccessMapper;
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
    private final ParoisseAccessMapper paroisseAccessMapper;
    private final PasswordEncoder passwordEncoder;
    private final ProfessionalEmailService professionalEmailService;

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.AUTH_USER_DETAILS, allEntries = true)
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
        user.setEmail(request.email());
        user.setTelephone(request.telephone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setIsGlobal(request.isGlobal());
        user.setIsActive(request.isActive());
        user.setStatusDel(false);

        if (!Boolean.TRUE.equals(request.isGlobal())
                && request.paroisses() != null
                && !request.paroisses().isEmpty()) {
            Paroisse firstParish = paroisseRepository
                    .findByPublicIdAndStatusDelFalse(request.paroisses().get(0).getParoisseId())
                    .orElse(null);
            if (firstParish != null) {
                professionalEmailService.assignToUser(user, firstParish);
            }
        }

        User savedUser = userRepository.save(user);

        if (!Boolean.TRUE.equals(savedUser.getIsGlobal())) {
            assignParoisses(savedUser, request.paroisses());
        }

        log.info("User created successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getId());
        return mapToResponse(savedUser);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.AUTH_USER_DETAILS, allEntries = true)
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

        boolean isSelf = user.getPublicId().equals(requester.getPublicId());

        if (isSelf && !Boolean.TRUE.equals(request.isActive())) {
            throw new BusinessRuleException("Vous ne pouvez pas désactiver votre propre compte");
        }
        ensureAnotherGlobalSuperAdminExistsBeforeDeactivation(user, request.isActive());
        ensureNotLastParishAdminBeforeDeactivation(user, request.isActive());
        enforceImmutableCredentialsOnUpdate(user, request, isSelf);

        if (isSelf) {
            // Identité, coordonnées et mot de passe n'appartiennent qu'au titulaire du compte.
            user.setNom(request.nom());
            user.setPrenom(request.prenom());
            applyEmailUpdate(user, request.email());
            user.setTelephone(request.telephone());
            if (request.password() != null && !request.password().isBlank()) {
                user.setPassword(passwordEncoder.encode(request.password()));
            }
        }

        user.setRole(request.role());
        user.setIsGlobal(request.isGlobal());
        user.setIsActive(request.isActive());

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", publicId);

        return mapToResponse(updatedUser);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.AUTH_USER_DETAILS, allEntries = true)
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
    @CacheEvict(cacheNames = CacheConfig.AUTH_USER_DETAILS, allEntries = true)
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
    @CacheEvict(cacheNames = CacheConfig.AUTH_USER_DETAILS, allEntries = true)
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
     * Mapping DTO dans la transaction (compatible open-in-view=false).
     */
    @Transactional(readOnly = true)
    public List<ParoisseAccessResponse> getUserParoisses(UUID userPublicId) {
        User user = findActiveUser(userPublicId);
        return paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(user)
                .stream()
                .map(paroisseAccessMapper::modelToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParoisseAccessResponse> getUserParoisses(UUID userPublicId, UUID requestedBy) {
        User requester = findActiveUser(requestedBy);
        User target = findActiveUser(userPublicId);
        assertCanAccessUser(requester, target);
        return paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(target)
                .stream()
                .map(paroisseAccessMapper::modelToDto)
                .toList();
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
    @CacheEvict(cacheNames = CacheConfig.AUTH_USER_DETAILS, allEntries = true)
    public void deleteUser(UUID userPublicId, UUID deletedBy) {
        User requester = findActiveUser(deletedBy);
        User user = findActiveUser(userPublicId);
        assertCanAccessUser(requester, user);

        if (user.getPublicId().equals(requester.getPublicId())) {
            throw new BusinessRuleException("Vous ne pouvez pas supprimer votre propre compte");
        }
        ensureAnotherGlobalSuperAdminExistsBeforeDeactivation(user, false);
        ensureNotLastParishAdminBeforeDeactivation(user, false);

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

        if (access.getRoleParoisse() == RoleParoisse.ADMIN
                && Boolean.TRUE.equals(access.getActive())
                && Boolean.TRUE.equals(user.getIsActive())) {
            long others = paroisseAccessRepository.countOtherActiveAdmins(
                    paroisse,
                    RoleParoisse.ADMIN,
                    user.getPublicId()
            );
            if (others == 0) {
                throw new BusinessRuleException(
                        "Impossible de retirer le dernier administrateur de « " + paroisse.getNom() + " »"
                );
            }
        }

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

        if (Boolean.TRUE.equals(request.isGlobal()) || request.role() == UserRole.SUPER_ADMIN
                || request.role() == UserRole.COMPTABLE) {
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
                normalizeEmail(request.email()),
                normalizeOptional(request.telephone(), 50, "Le téléphone ne doit pas dépasser 50 caractères"),
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

    /** Les coordonnées restent facultatives : une chaîne vide vaut « non renseigné ». */
    private String normalizeOptional(String value, int maxLength, String lengthMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.length() > maxLength) {
            throw new BusinessRuleException(lengthMessage);
        }
        return normalized;
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeOptional(
                value, 150, "L'adresse e-mail ne doit pas dépasser 150 caractères"
        );
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")) {
            throw new BusinessRuleException("L'adresse e-mail est invalide");
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
        boolean platformRole = request.role() == UserRole.SUPER_ADMIN
                || request.role() == UserRole.COMPTABLE;

        if (global != platformRole) {
            throw new BusinessRuleException(
                    "Les rôles SUPER_ADMIN / COMPTABLE et le statut global doivent être définis ensemble"
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

    /**
     * L'administrateur fondateur (patient 0) d'une paroisse ne peut pas être retiré
     * s'il est le dernier ADMIN actif de cette paroisse.
     */
    private void ensureNotLastParishAdminBeforeDeactivation(User user, Boolean requestedActive) {
        if (Boolean.TRUE.equals(requestedActive)) {
            return;
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            return;
        }

        List<ParoisseAccess> adminAccesses = paroisseAccessRepository.findByUserAndStatusDelFalse(user)
                .stream()
                .filter(access -> Boolean.TRUE.equals(access.getActive()))
                .filter(access -> access.getRoleParoisse() == RoleParoisse.ADMIN)
                .toList();

        for (ParoisseAccess access : adminAccesses) {
            long others = paroisseAccessRepository.countOtherActiveAdmins(
                    access.getParoisse(),
                    RoleParoisse.ADMIN,
                    user.getPublicId()
            );
            if (others == 0) {
                String parishName = access.getParoisse() != null ? access.getParoisse().getNom() : "cette paroisse";
                throw new BusinessRuleException(
                        "Impossible de désactiver le dernier administrateur de « " + parishName + " »"
                );
            }
        }
    }

    /**
     * Le nom d'utilisateur est immuable après création, et les informations
     * personnelles (nom, prénom, mot de passe) n'appartiennent qu'au titulaire :
     * un administrateur n'agit que sur le rôle, le périmètre et l'activation.
     */
    private void enforceImmutableCredentialsOnUpdate(User user, UserRequest request, boolean isSelf) {
        if (request.username() != null
                && !user.getUsername().equalsIgnoreCase(request.username().trim())) {
            throw new BusinessRuleException(
                    "Le nom d'utilisateur ne peut pas être modifié après la création du compte"
            );
        }

        if (isSelf) {
            return;
        }

        if (request.password() != null && !request.password().isBlank()) {
            throw new BusinessRuleException(
                    "Le mot de passe d'un autre utilisateur ne peut pas être modifié. "
                            + "Seul le titulaire du compte peut le changer."
            );
        }

        if (differs(user.getNom(), request.nom())
                || differs(user.getPrenom(), request.prenom())
                || differs(user.getEmail(), request.email())
                || differs(user.getTelephone(), request.telephone())) {
            throw new BusinessRuleException(
                    "Les informations personnelles d'un autre utilisateur ne peuvent pas être modifiées. "
                            + "Seul le titulaire du compte peut les mettre à jour."
            );
        }
    }

    /**
     * Compare sur la forme normalisée : une simple différence d'espacement ne
     * doit pas bloquer un administrateur qui ne touche qu'au rôle.
     */
    private boolean differs(String current, String requested) {
        if (requested == null) {
            return false;
        }
        return !collapseSpaces(requested).equals(collapseSpaces(current));
    }

    /**
     * E-mail professionnel généré à l'activation : immuable.
     * Les adresses hors domaine plateforme restent librement modifiables.
     */
    private void applyEmailUpdate(User user, String requestedEmail) {
        if (professionalEmailService.isProfessional(user.getEmail())) {
            if (requestedEmail != null && differs(user.getEmail(), requestedEmail)) {
                throw new BusinessRuleException(
                        "L'e-mail professionnel de la paroisse ne peut pas être modifié."
                );
            }
            return;
        }
        user.setEmail(requestedEmail);
    }

    private String collapseSpaces(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ");
    }

    /**
     * Met à jour les informations personnelles de l'utilisateur connecté.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.AUTH_USER_DETAILS, allEntries = true)
    public UserResponse updateOwnProfile(UUID publicId, ProfileUpdateRequest request) {
        User user = findActiveUser(publicId);
        user.setNom(normalizeRequired(request.nom(), 2, 100,
                "Le nom est obligatoire",
                "Le nom doit contenir entre 2 et 100 caractères"));
        user.setPrenom(normalizeRequired(request.prenom(), 2, 150,
                "Le prénom est obligatoire",
                "Le prénom doit contenir entre 2 et 150 caractères"));
        applyEmailUpdate(user, normalizeEmail(request.email()));
        user.setTelephone(normalizeOptional(
                request.telephone(), 50, "Le téléphone ne doit pas dépasser 50 caractères"
        ));
        log.info("Profil mis à jour en libre-service : {}", user.getUsername());
        return mapToResponse(userRepository.save(user));
    }

    /**
     * Change le mot de passe de l'utilisateur connecté, après vérification de
     * son mot de passe courant.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.AUTH_USER_DETAILS, allEntries = true)
    public void changeOwnPassword(UUID publicId, ChangePasswordRequest request) {
        User user = findActiveUser(publicId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessRuleException("Le mot de passe actuel est incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessRuleException("Le nouveau mot de passe doit différer de l'actuel");
        }
        validatePasswordLength(request.newPassword());

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(user.getTokenVersion() == null ? 1L : user.getTokenVersion() + 1L);
        userRepository.save(user);
        log.info("Mot de passe changé en libre-service : {}", user.getUsername());
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getPublicId(),
                user.getNom(),
                user.getPrenom(),
                user.getUsername(),
                user.getEmail(),
                user.getTelephone(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getIsActive(),
                user.getIsGlobal()
        );
    }
}
