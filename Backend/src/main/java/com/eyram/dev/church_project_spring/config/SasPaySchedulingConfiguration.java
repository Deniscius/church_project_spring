package com.eyram.dev.church_project_spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Keep bounded provider I/O away from the existing application cron jobs. */
@Configuration(proxyBeanMethods = false)
public class SasPaySchedulingConfiguration {
    @Bean(name = "taskScheduler")
    @Primary
    public ThreadPoolTaskScheduler applicationScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("app-scheduled-");
        return scheduler;
    }

    @Bean(name = "saspayScheduler")
    public ThreadPoolTaskScheduler sasPayScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("saspay-reconcile-");
        return scheduler;
    }
}
