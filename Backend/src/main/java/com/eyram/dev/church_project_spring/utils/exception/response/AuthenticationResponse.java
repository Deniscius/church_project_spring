package com.eyram.dev.church_project_spring.utils.exception.response;

import java.util.List;
import java.util.UUID;

public class AuthenticationResponse {
    private UUID id;
    private String fullName;
    private String username;
    private Long ministere;
    private String direction;
    private List<String> roles;
    private String token;
    private String type = "Bearer";


    public AuthenticationResponse(String accessToken, UUID id, String fullName, String username,
                                  Long ministere,String direction, List<String> roles) {
        this.token = accessToken;
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.direction = direction;
        this.ministere = ministere;
        this.roles = roles;
    }

    public String getAccessToken() {
        return token;
    }

    public void setAccessToken(String accessToken) {
        this.token = accessToken;
    }

    public String getTokenType() {
        return type;
    }

    public void setTokenType(String tokenType) {
        this.type = tokenType;
    }

    public UUID getId() {

        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Long getMinistere() {
        return ministere;
    }

    public void setMinistere(Long ministere) {
        this.ministere = ministere;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getRoles() {
        return roles;
    }


}
