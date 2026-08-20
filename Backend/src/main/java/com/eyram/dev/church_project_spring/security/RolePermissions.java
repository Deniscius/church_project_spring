package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.enums.UserRole;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Matrice RBAC centrale : un rôle applicatif regroupe des permissions métier.
 *
 * <p>Important : cette classe définit les capacités, pas le périmètre de données.
 * L'accès à une paroisse reste contrôlé séparément par {@link TenantAccessService}.</p>
 */
public final class RolePermissions {

    private static final Map<UserRole, Set<Permission>> ROLE_PERMISSIONS;

    static {
        EnumMap<UserRole, Set<Permission>> matrix = new EnumMap<>(UserRole.class);

        Set<Permission> parishRead = EnumSet.of(
                Permission.DASHBOARD_VIEW,
                Permission.DEMAND_READ,
                Permission.PAYMENT_READ,
                Permission.INVOICE_READ,
                Permission.SCHEDULE_READ,
                Permission.REQUEST_TYPE_READ,
                Permission.PRICING_READ,
                Permission.PARISH_READ,
                Permission.PROFILE_READ
        );

        EnumSet<Permission> superAdmin = EnumSet.of(
                Permission.SYSTEM_ADMIN,
                Permission.USER_MANAGE,
                Permission.PARISH_MANAGE,
                Permission.PARISH_SETTINGS_MANAGE,
                Permission.PARISH_ACCESS_MANAGE,
                Permission.PARISH_REGISTRATION_READ,
                Permission.PARISH_REGISTRATION_MANAGE,
                Permission.DEANERY_MANAGE,
                Permission.PAYMENT_TYPE_MANAGE,
                Permission.FINANCE_READ,
                Permission.SAAS_PLAN_READ,
                Permission.SAAS_PLAN_MANAGE,
                Permission.SUBSCRIPTION_READ,
                Permission.SUBSCRIPTION_ACTIVATE,
                Permission.DEMAND_EDIT,
                Permission.DEMAND_DELETE,
                Permission.DEMAND_VALIDATE,
                Permission.DEMAND_AUDIT,
                Permission.DEMAND_DATE_MANAGE,
                Permission.PAYMENT_MANAGE,
                Permission.PAYMENT_DELETE,
                Permission.TREASURY_READ,
                Permission.RECEIPT_MANAGE,
                Permission.INVOICE_MANAGE,
                Permission.SCHEDULE_MANAGE,
                Permission.CELEBRATION_MANAGE,
                Permission.CELEBRATION_SCHEDULE_MANAGE,
                Permission.REQUEST_TYPE_MANAGE,
                Permission.PRICING_MANAGE
        );
        superAdmin.addAll(parishRead);
        matrix.put(UserRole.SUPER_ADMIN, immutable(superAdmin));

        EnumSet<Permission> platformAccountant = EnumSet.of(
                Permission.FINANCE_READ,
                Permission.PAYOUT_MANAGE,
                Permission.SUBSCRIPTION_READ,
                Permission.SUBSCRIPTION_CHECKOUT,
                Permission.SUBSCRIPTION_ACTIVATE,
                Permission.SUBSCRIPTION_MANAGE,
                Permission.PARISH_READ,
                Permission.PARISH_REGISTRATION_READ,
                Permission.PARISH_REGISTRATION_MANAGE,
                Permission.DASHBOARD_VIEW,
                Permission.DEMAND_READ,
                Permission.DEMAND_AUDIT,
                Permission.PAYMENT_READ,
                Permission.SCHEDULE_READ,
                Permission.SCHEDULE_MANAGE,
                Permission.REQUEST_TYPE_READ,
                Permission.REQUEST_TYPE_MANAGE,
                Permission.PRICING_READ,
                Permission.PRICING_MANAGE,
                Permission.PROFILE_READ
        );
        matrix.put(UserRole.COMPTABLE, immutable(platformAccountant));

        EnumSet<Permission> admin = EnumSet.of(
                Permission.DEMAND_EDIT,
                Permission.DEMAND_DELETE,
                Permission.DEMAND_VALIDATE,
                Permission.DEMAND_AUDIT,
                Permission.DEMAND_DATE_MANAGE,
                Permission.PAYMENT_MANAGE,
                Permission.PAYMENT_DELETE,
                Permission.TREASURY_READ,
                Permission.TREASURY_MANAGE,
                Permission.SUBSCRIPTION_CHECKOUT,
                Permission.RECEIPT_MANAGE,
                Permission.INVOICE_MANAGE,
                Permission.SCHEDULE_MANAGE,
                Permission.CELEBRATION_MANAGE,
                Permission.CELEBRATION_SCHEDULE_MANAGE,
                Permission.REQUEST_TYPE_MANAGE,
                Permission.PRICING_MANAGE,
                Permission.USER_MANAGE,
                Permission.PARISH_SETTINGS_MANAGE
        );
        admin.addAll(parishRead);
        matrix.put(UserRole.ADMIN, immutable(admin));

        EnumSet<Permission> localAccountant = EnumSet.copyOf(parishRead);
        localAccountant.add(Permission.TREASURY_READ);
        localAccountant.add(Permission.DEMAND_AUDIT);
        matrix.put(UserRole.COMPTABLE_LOCAL, immutable(localAccountant));

        EnumSet<Permission> secretary = EnumSet.copyOf(parishRead);
        secretary.add(Permission.DEMAND_EDIT);
        secretary.add(Permission.PAYMENT_MANAGE);
        secretary.add(Permission.TREASURY_READ);
        secretary.add(Permission.CELEBRATION_MANAGE);
        secretary.add(Permission.CELEBRATION_SCHEDULE_MANAGE);
        matrix.put(UserRole.SECRETAIRE, immutable(secretary));

        EnumSet<Permission> priest = EnumSet.copyOf(parishRead);
        priest.add(Permission.DEMAND_VALIDATE);
        priest.add(Permission.CELEBRATION_MANAGE);
        matrix.put(UserRole.CURE, immutable(priest));

        ROLE_PERMISSIONS = Collections.unmodifiableMap(matrix);
    }

    private RolePermissions() {
    }

    public static Set<Permission> permissionsFor(UserRole role) {
        if (role == null) {
            return Set.of();
        }
        return ROLE_PERMISSIONS.getOrDefault(role, Set.of());
    }

    public static Set<String> authoritiesFor(UserRole role) {
        return permissionsFor(role).stream()
                .map(Permission::authority)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static boolean hasPermission(UserRole role, Permission permission) {
        return permission != null && permissionsFor(role).contains(permission);
    }

    public static boolean hasAuthority(UserRole role, String authority) {
        return authority != null && authoritiesFor(role).contains(authority);
    }

    public static boolean canRead(UserRole role) {
        return hasPermission(role, Permission.DEMAND_READ);
    }

    public static boolean canCreate(UserRole role) {
        return hasPermission(role, Permission.DEMAND_EDIT);
    }

    public static boolean canEdit(UserRole role) {
        return hasPermission(role, Permission.DEMAND_EDIT);
    }

    public static boolean canDelete(UserRole role) {
        return hasPermission(role, Permission.DEMAND_DELETE);
    }

    public static boolean canValidate(UserRole role) {
        return hasPermission(role, Permission.DEMAND_VALIDATE);
    }

    public static boolean isAdmin(UserRole role) {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN;
    }

    public static boolean isSuperAdmin(UserRole role) {
        return role == UserRole.SUPER_ADMIN;
    }

    public static boolean isAdminOrSuperAdmin(UserRole role) {
        return isAdmin(role);
    }

    /** Ordre d'affichage uniquement ; ne doit pas servir à autoriser une action. */
    public static UserRole[] getHierarchy() {
        return new UserRole[]{
                UserRole.SUPER_ADMIN,
                UserRole.COMPTABLE,
                UserRole.ADMIN,
                UserRole.COMPTABLE_LOCAL,
                UserRole.SECRETAIRE,
                UserRole.CURE
        };
    }

    private static Set<Permission> immutable(Set<Permission> permissions) {
        return Collections.unmodifiableSet(EnumSet.copyOf(permissions));
    }
}
