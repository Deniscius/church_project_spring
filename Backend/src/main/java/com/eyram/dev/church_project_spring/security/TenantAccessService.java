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

        return userRepository.findByPublicIdAndStatusDelFalse(userDetails.getPublicId())
                .orElseThrow(() -> new AccessDeniedException("Utilisateur introuvable ou inactif"));
    }

    public boolean canAccessParoisse(Paroisse paroisse) {
        User currentUser = getCurrentUser();

        if (Boolean.TRUE.equals(currentUser.getIsGlobal())) {
            return true;
        }

        return paroisseAccessRepository.existsByUserAndParoisseAndActiveTrueAndStatusDelFalse(currentUser, paroisse);
    }

    public void checkParoisseAccess(Paroisse paroisse) {
        if (!canAccessParoisse(paroisse)) {
            throw new AccessDeniedException("Accès refusé à cette paroisse");
        }
    }

    public boolean hasParoisseRole(Paroisse paroisse, RoleParoisse... roles) {
        User currentUser = getCurrentUser();

        if (Boolean.TRUE.equals(currentUser.getIsGlobal())) {
            return true;
        }

        return paroisseAccessRepository.findByUserAndParoisseAndStatusDelFalse(currentUser, paroisse)
                .filter(access -> Boolean.TRUE.equals(access.getActive()))
                .map(access -> {
                    for (RoleParoisse role : roles) {
                        if (access.getRoleParoisse() == role) {
                            return true;
                        }
                    }
                    return false;
                })
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
}
