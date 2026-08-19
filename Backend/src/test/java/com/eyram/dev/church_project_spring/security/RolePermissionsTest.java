package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolePermissionsTest {

    @Test
    void secretaryCanManageOperationalRequestsButCannotDeleteThem() {
        assertTrue(RolePermissions.hasPermission(UserRole.SECRETAIRE, Permission.DEMAND_EDIT));
        assertTrue(RolePermissions.hasPermission(UserRole.SECRETAIRE, Permission.PAYMENT_MANAGE));
        assertTrue(RolePermissions.hasPermission(UserRole.SECRETAIRE, Permission.CELEBRATION_SCHEDULE_MANAGE));
        assertFalse(RolePermissions.hasPermission(UserRole.SECRETAIRE, Permission.DEMAND_DELETE));
        assertFalse(RolePermissions.hasPermission(UserRole.SECRETAIRE, Permission.USER_MANAGE));
    }

    @Test
    void cureCanValidateAndMarkCelebrationWithoutEditingDemand() {
        assertTrue(RolePermissions.hasPermission(UserRole.CURE, Permission.DEMAND_VALIDATE));
        assertTrue(RolePermissions.hasPermission(UserRole.CURE, Permission.CELEBRATION_MANAGE));
        assertFalse(RolePermissions.hasPermission(UserRole.CURE, Permission.DEMAND_EDIT));
        assertFalse(RolePermissions.hasPermission(UserRole.CURE, Permission.PAYMENT_MANAGE));
    }

    @Test
    void localAccountantCanAuditWithoutMutatingPayments() {
        assertTrue(RolePermissions.hasPermission(UserRole.COMPTABLE_LOCAL, Permission.DEMAND_AUDIT));
        assertTrue(RolePermissions.hasPermission(UserRole.COMPTABLE_LOCAL, Permission.TREASURY_READ));
        assertFalse(RolePermissions.hasPermission(UserRole.COMPTABLE_LOCAL, Permission.PAYMENT_MANAGE));
    }

    @Test
    void parishAdminDoesNotReceivePlatformFinancePermissions() {
        assertTrue(RolePermissions.hasPermission(UserRole.ADMIN, Permission.USER_MANAGE));
        assertTrue(RolePermissions.hasPermission(UserRole.ADMIN, Permission.RECEIPT_MANAGE));
        assertFalse(RolePermissions.hasPermission(UserRole.ADMIN, Permission.FINANCE_MANAGE));
        assertFalse(RolePermissions.hasPermission(UserRole.ADMIN, Permission.SAAS_PLAN_MANAGE));
    }

    @Test
    void platformAccountantHasFinanceAndTemplateCatalogRightsButNotUserManagement() {
        assertTrue(RolePermissions.hasPermission(UserRole.COMPTABLE, Permission.FINANCE_MANAGE));
        assertTrue(RolePermissions.hasPermission(UserRole.COMPTABLE, Permission.SCHEDULE_MANAGE));
        assertTrue(RolePermissions.hasPermission(UserRole.COMPTABLE, Permission.DEMAND_AUDIT));
        assertFalse(RolePermissions.hasPermission(UserRole.COMPTABLE, Permission.USER_MANAGE));
        assertFalse(RolePermissions.hasPermission(UserRole.COMPTABLE, Permission.SAAS_PLAN_MANAGE));
    }

    @Test
    void principalContainsRoleAndGranularAuthorities() {
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setNom("Test");
        user.setPrenom("Admin");
        user.setUsername("admin-test");
        user.setPassword("encoded");
        user.setRole(UserRole.ADMIN);
        user.setIsActive(true);
        user.setIsGlobal(false);

        UserDetailsImpl principal = UserDetailsImpl.build(user, 42L);
        Set<String> authorities = principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());

        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains(Permission.DEMAND_EDIT.authority()));
        assertTrue(authorities.contains(Permission.USER_MANAGE.authority()));
        assertFalse(authorities.contains(Permission.FINANCE_MANAGE.authority()));
    }
}
