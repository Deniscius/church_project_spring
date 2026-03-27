package com.eyram.dev.church_project_spring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI churchProjectOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Church Project API")
                        .version("1.0.0")
                        .description("Documentation de l'API de gestion des demandes de messe")
                        .contact(new Contact()
                                .name("Eyram Dev")
                                .email("support@churchproject.com")));
    }
}