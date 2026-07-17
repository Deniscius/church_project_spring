package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class TenantAccessService {

    private final UserRepository userRepository;
    private final ParoisseAccessRepository paroisseAccessRepository;

    public TenantAccessService(UserRepository userRepository,
                               ParoisseAccessRepository paroisseAccessRepository) {
        this.userRepository = userRepository;
        this.paroisseAccessRepository = paroisseAccessRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Utilisateur non authentifié");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            throw new AccessDeniedException("Utilisateur non authentifié");
        }

        User currentUser = userRepository.findByPublicIdAndStatusDelFalse(userDetails.getPublicId())
                .orElseThrow(() -> new AccessDeniedException("Utilisateur introuvable"));

        if (!Boolean.TRUE.equals(currentUser.getIsActive())) {
            throw new AccessDeniedException("Utilisateur inactif");
        }

        if (currentUser.getRole() == null) {
            throw new AccessDeniedException("Utilisateur sans rôle valide");
        }

        return currentUser;
    }

    public boolean canAccessParoisse(Paroisse paroisse) {
        if (!isExistingParoisse(paroisse)) {
            return false;
        }

        User currentUser = getCurrentUser();

        if (Boolean.TRUE.equals(currentUser.getIsGlobal())) {
            return true;
        }

        return paroisseAccessRepository
                .existsByUserAndParoisseAndActiveTrueAndStatusDelFalse(currentUser, paroisse);
    }

    public void checkParoisseAccess(Paroisse paroisse) {
        if (!canAccessParoisse(paroisse)) {
            throw new AccessDeniedException("Accès refusé à cette paroisse");
        }
    }

    public boolean hasParoisseRole(Paroisse paroisse, RoleParoisse... roles) {
        if (!isExistingParoisse(paroisse) || roles == null || roles.length == 0) {
            return false;
        }

        User currentUser = getCurrentUser();

        if (Boolean.TRUE.equals(currentUser.getIsGlobal())) {
            return true;
        }

        return paroisseAccessRepository.findByUserAndParoisseAndStatusDelFalse(currentUser, paroisse)
                .filter(access -> Boolean.TRUE.equals(access.getActive()))
                .map(access -> access.getRoleParoisse())
                .filter(Objects::nonNull)
                .map(accessRole -> Arrays.stream(roles)
                        .filter(Objects::nonNull)
                        .anyMatch(role -> role == accessRole))
                .orElse(false);
    }

    public void checkParoisseRole(Paroisse paroisse, RoleParoisse... roles) {
        if (!hasParoisseRole(paroisse, roles)) {
            throw new AccessDeniedException("Vous n'avez pas les droits suffisants sur cette paroisse");
        }
    }

    public boolean isGlobalUser() {
        return Boolean.TRUE.equals(getCurrentUser().getIsGlobal());
    }

    private boolean isExistingParoisse(Paroisse paroisse) {
        return paroisse != null && !Boolean.TRUE.equals(paroisse.getStatusDel());
    }
}
