package com.eyram.dev.church_project_spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ApplicationTimeConfig {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Lome");

    @Bean
    Clock applicationClock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
