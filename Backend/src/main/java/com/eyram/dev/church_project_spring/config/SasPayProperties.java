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
  private String apiBaseUrl = "auto";
  private String appUrl = "http://localhost:5173";
  private String country = "TG";

  /** Trim copy/paste padding, but never silently remove quotes or embedded whitespace. */
  public String getApiKey() {
    return apiKey == null ? "" : apiKey.strip();
  }

  /** No network fallback: a key selects one API, unless an explicit matching URL is supplied. */
  public String getApiBaseUrl() {
    String configured = apiBaseUrl == null ? "" : apiBaseUrl.strip();
    if (configured.isBlank() || "auto".equalsIgnoreCase(configured)) {
      return getApiKey().startsWith("sk_test_")
          ? "https://sandbox-api.saspay.me/api/v1"
          : "https://api.saspay.me/api/v1";
    }
    return configured.replaceAll("/+$", "");
  }

  private int connectTimeoutMs = 3000, readTimeoutMs = 8000;
}
