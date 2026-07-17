package com.eyram.dev.church_project_spring.config;

import com.eyram.dev.church_project_spring.enums.UserRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin-seed")
public class AdminSeedProperties {

    /**
     * Désactivé par défaut pour éviter toute création involontaire d'un compte
     * administrateur en dehors du profil de développement.
     */
    private boolean enabled = false;

    private String username = "admin";

    /**
     * Aucun mot de passe par défaut n'est conservé dans le code source.
     * La valeur doit être fournie par APP_ADMIN_SEED_PASSWORD lorsque le seeder
     * est explicitement activé.
     */
    private String password;

    private String nom = "Administrateur";

    private String prenom = "Système";

    private UserRole role = UserRole.SUPER_ADMIN;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
