package com.eyram.dev.church_project_spring;

import com.eyram.dev.church_project_spring.config.AdminSeedProperties;
import com.eyram.dev.church_project_spring.config.DatabaseUrlBootstrap;
import com.eyram.dev.church_project_spring.config.DemandePaymentProperties;
import com.eyram.dev.church_project_spring.config.FedaPayProperties;
import com.eyram.dev.church_project_spring.config.JwtProperties;
import com.eyram.dev.church_project_spring.config.LocalDotEnvLoader;
import com.eyram.dev.church_project_spring.config.PlatformBillingProperties;
import com.eyram.dev.church_project_spring.config.RateLimitProperties;
import com.eyram.dev.church_project_spring.config.StorageProperties;
import com.eyram.dev.church_project_spring.config.TenantCatalogProperties;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EntityScan(basePackageClasses = Paroisse.class)
@EnableJpaAuditing
@EnableScheduling
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties({
        AdminSeedProperties.class,
        JwtProperties.class,
        FedaPayProperties.class,
        PlatformBillingProperties.class,
        TenantCatalogProperties.class,
        DemandePaymentProperties.class,
        StorageProperties.class,
        RateLimitProperties.class
})
public class ChurchProjectSpringApplication {

    public static void main(String[] args) {
        LocalDotEnvLoader.loadIfPresent();
        DatabaseUrlBootstrap.applyIfPresent();
        SpringApplication.run(ChurchProjectSpringApplication.class, args);
    }
}
