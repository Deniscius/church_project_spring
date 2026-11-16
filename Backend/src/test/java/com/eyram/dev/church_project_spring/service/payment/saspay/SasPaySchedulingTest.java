package com.eyram.dev.church_project_spring.service.payment.saspay;

import com.eyram.dev.church_project_spring.config.SasPaySchedulingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

class SasPaySchedulingTest {
    @Test void slowProviderWorkCannotBlockTheApplicationScheduler() throws Exception {
        try(var context=new AnnotationConfigApplicationContext(SasPaySchedulingConfiguration.class)) {
            var app=context.getBean("taskScheduler",ThreadPoolTaskScheduler.class);
            var payments=context.getBean("saspayScheduler",ThreadPoolTaskScheduler.class);
            assertThat(context.getBean(TaskScheduler.class)).isSameAs(app);
            assertThat(payments).isNotSameAs(app);
            var started=new CountDownLatch(1);var release=new CountDownLatch(1);var completed=new CountDownLatch(1);
            try {
                payments.submit(()->{started.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}});
                assertThat(started.await(2,TimeUnit.SECONDS)).isTrue();
                app.submit(completed::countDown);
                assertThat(completed.await(2,TimeUnit.SECONDS)).isTrue();
            } finally { release.countDown(); }
        }
    }
}
