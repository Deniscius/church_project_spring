package com.eyram.dev.church_project_spring.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.eyram.dev.church_project_spring.enums.UserRole;

/**
 * Gestion centralisée des permissions par rôle.
 * Hiérarchie : SUPER_ADMIN > ADMIN > SECRETAIRE > CURE
 * 
 * Note : Les fidèles n'ont pas de compte, ils accèdent via endpoints publics anonymes.
 */
public class RolePermissions {

    public static final String PERMISSION_READ = "read";
    public static final String PERMISSION_CREATE = "create";
    public static final String PERMISSION_EDIT = "edit";
    public static final String PERMISSION_DELETE = "delete";
    public static final String PERMISSION_ADMIN = "admin";
    public static final String PERMISSION_VALIDATE = "validate";
    public static final String PERMISSION_MANAGE_USERS = "manage_users";
    public static final String PERMISSION_MANAGE_SYSTEM = "manage_system";

    private static final Map<UserRole, Set<String>> ROLE_PERMISSIONS = new HashMap<>();

    static {
        // ✅ SUPER_ADMIN : accès complet
        ROLE_PERMISSIONS.put(UserRole.SUPER_ADMIN, Set.of(
                PERMISSION_READ,
                PERMISSION_CREATE,
                PERMISSION_EDIT,
                PERMISSION_DELETE,
                PERMISSION_ADMIN,
                PERMISSION_VALIDATE,
                PERMISSION_MANAGE_USERS,
                PERMISSION_MANAGE_SYSTEM
        ));

        // ✅ ADMIN : administrateur local
        ROLE_PERMISSIONS.put(UserRole.ADMIN, Set.of(
                PERMISSION_READ,
                PERMISSION_CREATE,
                PERMISSION_EDIT,
                PERMISSION_DELETE,
                PERMISSION_VALIDATE,
                PERMISSION_MANAGE_USERS
        ));

        // ✅ SECRETAIRE : gestion complète des demandes
        ROLE_PERMISSIONS.put(UserRole.SECRETAIRE, Set.of(
                PERMISSION_READ,
                PERMISSION_CREATE,
                PERMISSION_EDIT,
                PERMISSION_VALIDATE
        ));

        // ✅ CURE : consultation et validation
        ROLE_PERMISSIONS.put(UserRole.CURE, Set.of(
                PERMISSION_READ,
                PERMISSION_VALIDATE
        ));
    }

    /**
     * Retourne l'ensemble des permissions d'un rôle.
     */
    public static Set<String> getPermissions(UserRole role) {
        return ROLE_PERMISSIONS.getOrDefault(role, new HashSet<>());
    }

    /**
     * Vérifie si un rôle possède une permission.
     */
    public static boolean hasPermission(UserRole role, String permission) {
        return getPermissions(role).contains(permission);
    }

    /**
     * Vérifie si un rôle peut lire.
     */
    public static boolean canRead(UserRole role) {
        return hasPermission(role, PERMISSION_READ);
    }

    /**
     * Vérifie si un rôle peut créer.
     */
    public static boolean canCreate(UserRole role) {
        return hasPermission(role, PERMISSION_CREATE);
    }

    /**
     * Vérifie si un rôle peut éditer.
     */
    public static boolean canEdit(UserRole role) {
        return hasPermission(role, PERMISSION_EDIT);
    }

    /**
     * Vérifie si un rôle peut supprimer.
     */
    public static boolean canDelete(UserRole role) {
        return hasPermission(role, PERMISSION_DELETE);
    }

    /**
     * Vérifie si un rôle peut valider.
     */
    public static boolean canValidate(UserRole role) {
        return hasPermission(role, PERMISSION_VALIDATE);
    }

    /**
     * Vérifie si un rôle peut administrer.
     */
    public static boolean isAdmin(UserRole role) {
        return hasPermission(role, PERMISSION_ADMIN) || 
               hasPermission(role, PERMISSION_MANAGE_SYSTEM);
    }

    /**
     * Vérifie si un rôle a accès administrateur global.
     */
    public static boolean isSuperAdmin(UserRole role) {
        return role == UserRole.SUPER_ADMIN;
    }

    /**
     * Vérifie si un rôle a accès administrateur (local ou global).
     */
    public static boolean isAdminOrSuperAdmin(UserRole role) {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN;
    }

    /**
     * Retourne la liste des rôles en ordre hiérarchique.
     */
    public static UserRole[] getHierarchy() {
        return new UserRole[]{
                UserRole.SUPER_ADMIN,
                UserRole.ADMIN,
                UserRole.SECRETAIRE,
                UserRole.CURE
        };
    }
}
