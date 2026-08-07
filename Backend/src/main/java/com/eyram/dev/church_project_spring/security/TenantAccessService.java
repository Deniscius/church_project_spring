package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.service.tenant.TenantCatalogBootstrapService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
    private final TenantCatalogBootstrapService tenantCatalogBootstrapService;

    public TenantAccessService(UserRepository userRepository,
                               ParoisseAccessRepository paroisseAccessRepository,
                               TenantCatalogBootstrapService tenantCatalogBootstrapService) {
        this.userRepository = userRepository;
        this.paroisseAccessRepository = paroisseAccessRepository;
        this.tenantCatalogBootstrapService = tenantCatalogBootstrapService;
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetailsImpl;
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

        // Évite un aller-retour DB pour les super-admins globaux.
        if (isGlobalFromPrincipal()) {
            return true;
        }

        User currentUser = getCurrentUser();
        return paroisseAccessRepository
                .existsByUserAndParoisseAndActiveTrueAndStatusDelFalse(currentUser, paroisse);
    }

    public void checkParoisseAccess(Paroisse paroisse) {
        if (!canAccessParoisse(paroisse)) {
            throw new AccessDeniedException("Accès refusé à cette paroisse");
        }
    }

    /**
     * Écriture du catalogue métier (horaires / types / forfaits).
     * Le COMPTABLE ne peut modifier que la paroisse modèle SaaS.
     */
    public void checkCatalogWriteAccess(Paroisse paroisse) {
        checkParoisseAccess(paroisse);
        if (hasAuthority("ROLE_" + UserRole.COMPTABLE.name())
                && !hasAuthority("ROLE_" + UserRole.SUPER_ADMIN.name())
                && !tenantCatalogBootstrapService.isTemplateParoisse(paroisse)) {
            throw new AccessDeniedException(
                    "Le comptable ne peut modifier que le catalogue modèle de la plateforme"
            );
        }
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    public boolean hasParoisseRole(Paroisse paroisse, RoleParoisse... roles) {
        if (!isExistingParoisse(paroisse) || roles == null || roles.length == 0) {
            return false;
        }

        if (isGlobalFromPrincipal()) {
            return true;
        }

        User currentUser = getCurrentUser();

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
        if (!isAuthenticated()) {
            return false;
        }
        return isGlobalFromPrincipal();
    }

    private boolean isGlobalFromPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl details)) {
            return false;
        }
        return details.isGlobal();
    }

    private boolean isExistingParoisse(Paroisse paroisse) {
        return paroisse != null && !Boolean.TRUE.equals(paroisse.getStatusDel());
    }
}
