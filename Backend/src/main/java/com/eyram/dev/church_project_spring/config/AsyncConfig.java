package com.eyram.dev.church_project_spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        return build("mail-", 2, 8, 200);
    }

    /** Traitement webhooks paiement hors thread HTTP. */
    @Bean(name = "webhookExecutor")
    public Executor webhookExecutor() {
        return build("webhook-", 2, 6, 300);
    }

    /** Génération PDF / exports lourds. */
    @Bean(name = "pdfExecutor")
    public Executor pdfExecutor() {
        return build("pdf-", 1, 4, 50);
    }

    private static Executor build(String prefix, int core, int max, int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setThreadNamePrefix(prefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
