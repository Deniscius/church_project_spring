package com.eyram.dev.church_project_spring;

import com.eyram.dev.church_project_spring.config.AdminSeedProperties;
import com.eyram.dev.church_project_spring.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties({AdminSeedProperties.class, JwtProperties.class})
public class ChurchProjectSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChurchProjectSpringApplication.class, args);
    }
}
