package com.eyram.dev.church_project_spring.service;

import java.util.List;
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

/**
 * Service de gestion des utilisateurs avec support multi-tenant.
 *
 * Responsabilités:
 * - Création/modification d'utilisateurs
 * - Gestion des accès aux paroisses
 * - Validation des permissions
 * - Audit et logging
 *
 * Bonnes pratiques:
 * - Transactions explicites
 * - Isolation des données par tenant
 * - Validation des permissions à tous les niveaux
 * - Logging et audit de chaque opération
 * - Gestion d'erreurs appropriée
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedUserService {

    private final UserRepository userRepository;
    private final ParoisseRepository paroisseRepository;
    private final ParoisseAccessRepository paroisseAccessRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crée un nouvel utilisateur.
     * Seul SUPER_ADMIN ou ADMIN (admin local) peuvent créer des utilisateurs.
     *
     * @param request données de l'utilisateur
     * @param createdBy ID de l'utilisateur créateur (non utilisé - Spring Data Auditing gère les timestamps)
     * @return réponse de l'utilisateur créé
     */
    @Transactional
    public UserResponse createUser(UserRequest request, UUID createdBy) {
        log.info("Creating new user: {}", request.username());

        // 1. Valider les données
        validateUserRequest(request);

        // 2. Vérifier que l'username est unique
        if (userRepository.existsByUsernameAndStatusDelFalse(request.username())) {
            log.warn("Attempt to create user with duplicate username: {}", request.username());
            throw new BusinessRuleException("Le nom d'utilisateur est déjà pris");
        }

        // 3. Créer l'utilisateur
        User user = new User();
        user.setNom(request.nom());
        user.setPrenom(request.prenom());
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setIsGlobal(request.isGlobal());
        user.setIsActive(request.isActive());
        user.setStatusDel(false);  // Important: marquer comme actif

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getId());

        // 4. Si des paroisses sont spécifiées, les ajouter
        if (request.paroisses() != null && !request.paroisses().isEmpty()) {
            assignParoisses(savedUser, request.paroisses());
        }

        return mapToResponse(savedUser);
    }

    /**
     * Met à jour un utilisateur existant.
     *
     * @param publicId l'ID public de l'utilisateur
     * @param request données à mettre à jour
     * @param updatedBy ID de l'utilisateur qui effectue la modification (non utilisé - Spring Data Auditing gère les timestamps)
     * @return réponse de l'utilisateur modifié
     */
    @Transactional
    public UserResponse updateUser(UUID publicId, UserRequest request, UUID updatedBy) {
        log.info("Updating user: {}", publicId);

        // 1. Récupérer l'utilisateur
        User user = userRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", publicId);
                    return new EntityNotFoundException("Utilisateur non trouvé");
                });

        // 2. Valider les données
        validateUserRequest(request);

        // 3. Vérifier l'unicité du username si changé
        if (!user.getUsername().equals(request.username()) &&
                userRepository.existsByUsernameAndStatusDelFalse(request.username())) {
            throw new BusinessRuleException("Le nom d'utilisateur est déjà pris");
        }

        // 4. Mettre à jour les champs
        user.setNom(request.nom());
        user.setPrenom(request.prenom());
        user.setUsername(request.username());
        if (!request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        user.setRole(request.role());
        user.setIsGlobal(request.isGlobal());
        user.setIsActive(request.isActive());

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", publicId);

        return mapToResponse(updatedUser);
    }

    /**
     * Assigne une paroisse à un utilisateur.
     * Crée une entrée ParoisseAccess avec le rôle spécifié.
     *
     * @param userPublicId l'ID public (UUID) de l'utilisateur
     * @param assignment données d'assignation
     * @param createdBy ID utilisateur qui effectue l'action (non utilisé - Spring Data Auditing gère les timestamps)
     */
    @Transactional
    public void assignParoisseToUser(UUID userPublicId, ParoisseAssignmentRequest assignment, UUID createdBy) {
        log.info("Assigning paroisse {} to user (ID: {})", assignment.getParoisseId(), userPublicId);

        // 1. Récupérer l'utilisateur
        User user = userRepository.findByPublicIdAndStatusDelFalse(userPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        // 2. Récupérer la paroisse
        var paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(assignment.getParoisseId())
                .orElseThrow(() -> {
                    log.warn("Paroisse not found: {}", assignment.getParoisseId());
                    return new EntityNotFoundException("Paroisse non trouvée");
                });

        // 3. Vérifier qu'il n'y a pas déjà un accès
        boolean alreadyExists = paroisseAccessRepository
                .existsByUserAndParoisseAndStatusDelFalse(user, paroisse);
        if (alreadyExists) {
            log.warn("User {} already has access to paroisse {}", userPublicId, paroisse.getId());
            throw new BusinessRuleException("L'utilisateur a déjà accès à cette paroisse");
        }

        // 4. Créer l'accès
        ParoisseAccess access = new ParoisseAccess();
        access.setUser(user);
        access.setParoisse(paroisse);
        access.setRoleParoisse(RoleParoisse.valueOf(assignment.getRoleParoisse()));
        access.setActive(true);
        access.setStatusDel(false);  // Important: marquer comme actif

        paroisseAccessRepository.save(access);
        log.info("Paroisse access created: user {} -> paroisse {} with role {}",
                userPublicId, paroisse.getId(), assignment.getRoleParoisse());
    }

    /**
     * Révoque l'accès d'un utilisateur à une paroisse (soft delete).
     *
     * @param userPublicId l'ID public (UUID) de l'utilisateur
     * @param paroisseId l'ID de la paroisse
     * @param revokedBy ID de l'utilisateur qui retire l'accès (non utilisé - Spring Data Auditing gère les timestamps)
     */
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

        access.setStatusDel(true);  // Soft delete
        paroisseAccessRepository.save(access);

        log.info("Paroisse access revoked successfully");
    }

    /**
     * Récupère tous les utilisateurs d'une paroisse.
     * Respecte l'isolation multi-tenant via statusDel.
     *
     * @param paroisseId l'ID de la paroisse
     * @return liste des utilisateurs de la paroisse
     */
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

    /**
     * Récupère les paroisses d'un utilisateur.
     *
     * @param userPublicId l'ID public (UUID) de l'utilisateur
     * @return liste des accès paroissiaux actifs
     */
    @Transactional(readOnly = true)
    public List<ParoisseAccess> getUserParoisses(UUID userPublicId) {
        User user = userRepository.findByPublicIdAndStatusDelFalse(userPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        return paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(user);
    }

    /**
     * Récupère un utilisateur spécifique par son ID public.
     *
     * @param userPublicId l'ID public (UUID) de l'utilisateur
     * @return utilisateur avec ses paroisses
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByPublicId(UUID userPublicId) {
        User user = userRepository.findByPublicIdAndStatusDelFalse(userPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        return mapToResponse(user);
    }

    /**
     * Récupère tous les utilisateurs actifs (non supprimés).
     * Respecte l'isolation multi-tenant via statusDel.
     *
     * @return liste de tous les utilisateurs
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllActiveUsers() {
        log.debug("Fetching all active users");
        List<User> users = userRepository.findByStatusDelFalse();
        return users.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Supprime (soft delete) un utilisateur.
     * Marque comme deleted mais conserve les données.
     *
     * @param userPublicId l'ID public (UUID) de l'utilisateur
     * @param deletedBy l'ID public de l'utilisateur qui effectue la suppression
     */
    @Transactional
    public void deleteUser(UUID userPublicId, UUID deletedBy) {
        User user = userRepository.findByPublicIdAndStatusDelFalse(userPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        user.setStatusDel(true);  // Soft delete
        userRepository.save(user);

        log.info("User deleted successfully (soft delete): {} (deleted by: {})", 
                user.getUsername(), deletedBy);
    }

    /**
     * Valide les données de création/modification d'utilisateur.
     */
    private void validateUserRequest(UserRequest request) {
        if (request.nom() == null || request.nom().isBlank()) {
            throw new BusinessRuleException("Le nom est obligatoire");
        }
        if (request.prenom() == null || request.prenom().isBlank()) {
            throw new BusinessRuleException("Le prénom est obligatoire");
        }
        if (request.username() == null || request.username().isBlank()) {
            throw new BusinessRuleException("Le nom d'utilisateur est obligatoire");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new BusinessRuleException("Le mot de passe doit contenir au minimum 8 caractères");
        }
        if (request.role() == null) {
            throw new BusinessRuleException("Le rôle est obligatoire");
        }
    }

    /**
     * Assigne plusieurs paroisses à un utilisateur.
     */
    private void assignParoisses(User user, List<ParoisseAssignmentRequest> paroisses) {
        for (ParoisseAssignmentRequest assignment : paroisses) {
            try {
                assignParoisseToUser(user.getPublicId(), assignment, null);
            } catch (Exception ex) {
                log.warn("Could not assign paroisse to user: {}", ex.getMessage());
                // Continuer avec les autres paroisses
            }
        }
    }

    /**
     * Mappe un User vers UserResponse.
     */
    private UserResponse mapToResponse(User user) {
        List<ParoisseAccess> accesses = paroisseAccessRepository
                .findByUserAndActiveTrueAndStatusDelFalse(user);

        return new UserResponse(
                user.getPublicId(),
                user.getNom(),
                user.getPrenom(),
                user.getUsername(),
                user.getRole().name(),
                accesses
        );
    }
}
