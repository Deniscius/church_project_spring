package com.eyram.dev.church_project_spring.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAssignmentRequest;
import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service de gestion des paroisses et des opérations Super Admin.
 *
 * Responsabilités:
 * - Gestion des paroisses (CRUD)
 * - Assignation des admins locaux aux paroisses
 * - Gestion globale des entités (TypeDemande, ForfaitTarif, etc.)
 * - Audit et logging des opérations sensibles
 *
 * Accès: SUPER_ADMIN uniquement (sauf si autorisé autrement)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParoisseManagementService {

    private final ParoisseRepository paroisseRepository;
    private final EnhancedUserService userService;

    /**
     * Crée une nouvelle paroisse.
     * Accès: SUPER_ADMIN
     *
     * @param paroisse la paroisse à créer
     * @param createdBy ID de l'utilisateur qui crée (non utilisé - Spring Data Auditing gère les timestamps)
     * @return la paroisse créée
     */
    @Transactional
    public Paroisse createParoisse(Paroisse paroisse, UUID createdBy) {
        log.info("Creating paroisse: {}", paroisse.getNom());

        // Validations
        if (paroisse.getNom() == null || paroisse.getNom().isBlank()) {
            throw new BusinessRuleException("Le nom de la paroisse est obligatoire");
        }
        if (paroisse.getAdresse() == null || paroisse.getAdresse().isBlank()) {
            throw new BusinessRuleException("L'adresse est obligatoire");
        }
        if (paroisse.getLocalite() == null) {
            throw new BusinessRuleException("La localité est obligatoire");
        }

        // Vérifier l'unicité
        List<Paroisse> existing = paroisseRepository.findAllByStatusDelFalse();
        if (existing.stream().anyMatch(p -> p.getNom().equalsIgnoreCase(paroisse.getNom()))) {
            log.warn("Attempt to create duplicate paroisse: {}", paroisse.getNom());
            throw new BusinessRuleException("Une paroisse portant ce nom existe déjà");
        }

        paroisse.setStatusDel(false);  // Important: marquer comme actif
        paroisse.setIsActive(true);

        Paroisse created = paroisseRepository.save(paroisse);
        log.info("Paroisse created successfully: {} (ID: {})", created.getNom(), created.getId());

        return created;
    }

    /**
     * Met à jour une paroisse existante.
     * Accès: SUPER_ADMIN + admin local de la paroisse
     *
     * @param paroisseId l'ID de la paroisse
     * @param updatedParoisse les données mises à jour
     * @param updatedBy ID de l'utilisateur qui effectue la mise à jour (non utilisé - Spring Data Auditing gère les timestamps)
     * @return la paroisse mise à jour
     */
    @Transactional
    public Paroisse updateParoisse(Long paroisseId, Paroisse updatedParoisse, UUID updatedBy) {
        log.info("Updating paroisse: {}", paroisseId);

        Paroisse paroisse = paroisseRepository.findById(paroisseId)
                .orElseThrow(() -> {
                    log.warn("Paroisse not found: {}", paroisseId);
                    return new EntityNotFoundException("Paroisse non trouvée");
                });

        // Mettre à jour les champs
        if (updatedParoisse.getNom() != null && !updatedParoisse.getNom().isBlank()) {
            paroisse.setNom(updatedParoisse.getNom());
        }
        if (updatedParoisse.getAdresse() != null && !updatedParoisse.getAdresse().isBlank()) {
            paroisse.setAdresse(updatedParoisse.getAdresse());
        }
        if (updatedParoisse.getEmail() != null) {
            paroisse.setEmail(updatedParoisse.getEmail());
        }
        if (updatedParoisse.getTelephone() != null) {
            paroisse.setTelephone(updatedParoisse.getTelephone());
        }

        Paroisse updated = paroisseRepository.save(paroisse);
        log.info("Paroisse updated successfully: {}", paroisseId);

        return updated;
    }

    /**
     * Désactive une paroisse (soft delete via statusDel).
     * Accès: SUPER_ADMIN
     *
     * @param paroisseId l'ID de la paroisse
     * @param deactivatedBy ID de l'utilisateur qui désactive (non utilisé - Spring Data Auditing gère les timestamps)
     */
    @Transactional
    public void deactivateParoisse(Long paroisseId, UUID deactivatedBy) {
        log.info("Deactivating paroisse: {}", paroisseId);

        Paroisse paroisse = paroisseRepository.findById(paroisseId)
                .orElseThrow(() -> new EntityNotFoundException("Paroisse non trouvée"));

        paroisse.setStatusDel(true);  // Soft delete
        paroisse.setIsActive(false);

        paroisseRepository.save(paroisse);
        log.info("Paroisse deactivated successfully: {}", paroisseId);
    }

    /**
     * Assigne un administrateur local à une paroisse.
     * Crée un utilisateur avec role=ADMIN et l'assigne à la paroisse.
     *
     * Accès: SUPER_ADMIN
     *
     * @param paroisseId l'ID de la paroisse
     * @param userRequest données du nouvel admin
     * @param assignedBy ID du super admin qui assigne (non utilisé - Spring Data Auditing gère les timestamps)
     * @return l'utilisateur créé
     */
    @Transactional
    public UserResponse assignAdminToParoisse(Long paroisseId, UserRequest userRequest, UUID assignedBy) {
        log.info("Assigning admin to paroisse: {}", paroisseId);

        // 1. Vérifier que la paroisse existe
        var paroisse = paroisseRepository.findById(paroisseId)
                .orElseThrow(() -> {
                    log.warn("Paroisse not found: {}", paroisseId);
                    return new EntityNotFoundException("Paroisse non trouvée");
                });

        // 2. Créer l'utilisateur avec role ADMIN
        UserRequest adminRequest = new UserRequest(
                userRequest.nom(),
                userRequest.prenom(),
                userRequest.username(),
                userRequest.password(),
                false,  // isGlobal = false (admin local)
                true,   // isActive = true
                UserRole.ADMIN,
                null    // pas de paroisses dans la requête initiale
        );

        // 3. Créer l'utilisateur
        UserResponse createdUser = userService.createUser(adminRequest, assignedBy);

        // 4. Assigner la paroisse avec le rôle ADMIN
        ParoisseAssignmentRequest assignment = ParoisseAssignmentRequest.builder()
                .paroisseId(paroisse.getPublicId())
                .roleParoisse("ADMIN")
                .build();

        userService.assignParoisseToUser(createdUser.publicId(), assignment, assignedBy);

        log.info("Admin assigned to paroisse successfully: user {} -> paroisse {}", 
                createdUser.publicId(), paroisseId);

        return createdUser;
    }

    /**
     * Récupère tous les administrateurs d'une paroisse.
     *
     * @param paroisseId l'ID de la paroisse
     * @return liste des administrateurs de la paroisse
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getParoisseAdmins(Long paroisseId) {
        log.debug("Fetching admins for paroisse: {}", paroisseId);
        return userService.getUsersByParoisse(paroisseId);
    }

    /**
     * Obtient toutes les paroisses actives (statusDel = false).
     * Accès: Lecture publique (tous les authentifiés)
     *
     * @return liste des paroisses
     */
    @Transactional(readOnly = true)
    public List<Paroisse> getAllActiveParoisses() {
        return paroisseRepository.findAllByStatusDelFalse();
    }

    /**
     * Obtient une paroisse par son ID.
     *
     * @param paroisseId l'ID de la paroisse
     * @return la paroisse
     */
    @Transactional(readOnly = true)
    public Paroisse getParoisseById(Long paroisseId) {
        return paroisseRepository.findById(paroisseId)
                .orElseThrow(() -> new EntityNotFoundException("Paroisse non trouvée"));
    }

    /**
     * Vérifie si un utilisateur est admin d'une paroisse.
     *
     * @param userPublicId l'ID public (UUID) de l'utilisateur
     * @param paroisseId l'ID de la paroisse
     * @return true si admin
     */
    @Transactional(readOnly = true)
    public boolean isParoisseAdmin(UUID userPublicId, Long paroisseId) {
        var userParoisses = userService.getUserParoisses(userPublicId);
        return userParoisses.stream()
                .anyMatch(pa -> pa.getParoisse().getId().equals(paroisseId) &&
                        pa.getRoleParoisse().name().equals("ADMIN"));
    }

    /**
     * Récupère le nombre de paroisses gérées.
     *
     * @return nombre de paroisses actives
     */
    @Transactional(readOnly = true)
    public long getActiveParoisseCount() {
        return paroisseRepository.count();
    }
}
