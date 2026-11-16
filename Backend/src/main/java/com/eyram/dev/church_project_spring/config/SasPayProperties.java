package com.eyram.dev.church_project_spring.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "saspay")
public class SasPayProperties {
    private boolean enabled = false;
    private String apiKey = "", webhookSecret = "";
    private String apiBaseUrl = "https://api.saspay.me/api/v1";
    private String appUrl = "http://localhost:5173";
    private String country = "TG";
    private int connectTimeoutMs = 3000, readTimeoutMs = 8000;
}
