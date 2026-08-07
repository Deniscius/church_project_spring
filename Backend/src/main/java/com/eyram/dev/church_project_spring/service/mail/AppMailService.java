package com.eyram.dev.church_project_spring.service.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Envoi d'e-mails transactionnels. Si SMTP n'est pas configuré, le contenu
 * est journalisé (pratique en développement) sans faire échouer le parcours.
 */
@Slf4j
@Service
public class AppMailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public AppMailService(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.mail.from:noreply@missanye.com}") String from
    ) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /** Envoi synchrone (tests / cas où l'appelant doit attendre l'échec SMTP). */
    public void sendText(String to, String subject, String body) {
        doSend(to, subject, body);
    }

    /** Envoi asynchrone — ne bloque pas le thread HTTP (OTP, identifiants). */
    @Async("mailExecutor")
    public void sendTextAsync(String to, String subject, String body) {
        try {
            doSend(to, subject, body);
        } catch (Exception ex) {
            log.error("Échec envoi e-mail async vers {}: {}", to, ex.getMessage());
        }
    }

    private void doSend(String to, String subject, String body) {
        if (!StringUtils.hasText(to)) {
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("""
                    [MAIL-DEV] SMTP non configuré — message non expédié
                    To: {}
                    Subject: {}
                    ---
                    {}
                    ---
                    """, to, subject, body);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to.trim());
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
        log.info("E-mail envoyé à {}", to);
    }
}
