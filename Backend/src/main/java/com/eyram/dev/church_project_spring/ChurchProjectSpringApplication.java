package com.eyram.dev.church_project_spring;

import com.eyram.dev.church_project_spring.config.AdminSeedProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties(AdminSeedProperties.class)
public class ChurchProjectSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChurchProjectSpringApplication.class, args);
    }
}
