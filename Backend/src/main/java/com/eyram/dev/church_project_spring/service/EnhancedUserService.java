package com.eyram.dev.church_project_spring.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

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
        log.info("Creating new user: {}", request.username());

        validateCommonUserRequest(request);
        validatePasswordForCreate(request.password());
        validateTenantAssignmentForCreate(request);

        if (userRepository.existsByUsernameAndStatusDelFalse(request.username())) {
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
        log.info("Updating user: {}", publicId);

        User user = userRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", publicId);
                    return new EntityNotFoundException("Utilisateur non trouvé");
                });

        validateCommonUserRequest(request);
        validatePasswordForUpdate(request.password());

        if (!user.getUsername().equals(request.username())
                && userRepository.existsByUsernameAndStatusDelFalse(request.username())) {
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

        User user = userRepository.findByPublicIdAndStatusDelFalse(userPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        if (Boolean.TRUE.equals(user.getIsGlobal())) {
            throw new BusinessRuleException(
                    "Un utilisateur global ne doit pas être assigné à une paroisse"
            );
        }

        assignParoisse(user, assignment);
    }

    @Transactional
    public void revokeParoisseAccess(UUID userPublicId, Long paroisseId, UUID revokedBy) {
        log.info("Revoking paroisse access for user {} -> paroisse {}", userPublicId, paroisseId);

        User user = userRepository.findByPublicIdAndStatusDelFalse(userPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        Paroisse paroisse = paroisseRepository.findById(paroisseId)
                .orElseThrow(() -> new EntityNotFoundException("Paroisse non trouvée"));

        ParoisseAccess access = paroisseAccessRepository
                .findByUserAndParoisseAndStatusDelFalse(user, paroisse)
                .orElseThrow(() -> new EntityNotFoundException("Accès à la paroisse non trouvé"));

        access.setActive(false);
        access.setStatusDel(true);
        paroisseAccessRepository.save(access);

        log.info("Paroisse access revoked successfully");
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByParoisse(Long paroisseId) {
        log.debug("Fetching users for paroisse: {}", paroisseId);

        Paroisse paroisse = paroisseRepository.findById(paroisseId)
                .orElseThrow(() -> new EntityNotFoundException("Paroisse non trouvée"));

        List<ParoisseAccess> accesses = paroisseAccessRepository
                .findByParoisseAndStatusDelFalse(paroisse);

        return accesses.stream()
                .filter(access -> Boolean.TRUE.equals(access.getActive()))
                .map(access -> mapToResponse(access.getUser()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ParoisseAccess> getUserParoisses(UUID userPublicId) {
        User user = userRepository.findByPublicIdAndStatusDelFalse(userPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        return paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByPublicId(UUID userPublicId) {
        User user = userRepository.findByPublicIdAndStatusDelFalse(userPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllActiveUsers() {
        log.debug("Fetching all active users");
        return userRepository.findByStatusDelFalse()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(UUID userPublicId, UUID deletedBy) {
        User user = userRepository.findByPublicIdAndStatusDelFalse(userPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        user.setIsActive(false);
        user.setStatusDel(true);
        userRepository.save(user);

        log.info(
                "User deleted successfully (soft delete): {} (deleted by: {})",
                user.getUsername(),
                deletedBy
        );
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

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getPublicId(),
                user.getNom(),
                user.getPrenom(),
                user.getUsername(),
                user.getRole() != null ? user.getRole().name() : null
        );
    }
}
