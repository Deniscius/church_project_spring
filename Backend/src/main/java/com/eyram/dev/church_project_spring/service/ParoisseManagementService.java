package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAssignmentRequest;
import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Opérations administratives qui ne font pas partie du CRUD canonique des
 * paroisses : affectation d'administrateurs et statistiques globales.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParoisseManagementService {

    private final ParoisseRepository paroisseRepository;
    private final EnhancedUserService userService;

    public UserResponse assignAdminToParoisse(
            UUID paroissePublicId,
            UserRequest userRequest,
            UUID assignedBy
    ) {
        Paroisse paroisse = findActiveParoisse(paroissePublicId);

        ParoisseAssignmentRequest assignment = ParoisseAssignmentRequest.builder()
                .paroisseId(paroisse.getPublicId())
                .roleParoisse("ADMIN")
                .build();

        UserRequest adminRequest = new UserRequest(
                userRequest.nom(),
                userRequest.prenom(),
                userRequest.username(),
                userRequest.email(),
                userRequest.telephone(),
                userRequest.password(),
                false,
                true,
                UserRole.ADMIN,
                List.of(assignment)
        );

        UserResponse createdUser = userService.createUser(adminRequest, assignedBy);
        log.info(
                "Administrateur {} affecté à la paroisse {}",
                createdUser.publicId(),
                paroisse.getPublicId()
        );
        return createdUser;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getParoisseAdmins(UUID paroissePublicId) {
        Paroisse paroisse = findActiveParoisse(paroissePublicId);
        return userService.getUsersByParoisse(paroisse.getPublicId()).stream()
                .filter(user -> UserRole.ADMIN.name().equals(user.role()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long getActiveParoisseCount() {
        return paroisseRepository.countByStatusDelFalseAndIsActiveTrue();
    }

    private Paroisse findActiveParoisse(UUID publicId) {
        if (publicId == null) {
            throw new IllegalArgumentException("L'identifiant de la paroisse est obligatoire");
        }
        return paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)
                .filter(paroisse -> Boolean.TRUE.equals(paroisse.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse non trouvée"));
    }
}
