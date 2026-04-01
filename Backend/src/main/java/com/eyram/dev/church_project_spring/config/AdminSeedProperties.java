package com.eyram.dev.church_project_spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin-seed")
public class AdminSeedProperties {

    /**
     * Désactiver en production une fois le compte créé, ou utiliser un import SQL contrôlé.
     */
    private boolean enabled = true;

    private String username = "admin";

    /**
     * À surcharger via variable d'environnement en tout environnement réel.
     */
    private String password = "ChangeMe123!";

    private String nom = "Administrateur";

    private String prenom = "Système";

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
}
