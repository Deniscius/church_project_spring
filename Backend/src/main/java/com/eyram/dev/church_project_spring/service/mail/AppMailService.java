package com.eyram.dev.church_project_spring.service.mail;

import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * E-mails transactionnels.
 * En prod ({@code app.mail.fail-closed=true}) : absence de SMTP = échec (pas de log du corps).
 * En dev : journalisation sans secrets sensibles si possible.
 */
@Slf4j
@Service
public class AppMailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final boolean failClosed;

    public AppMailService(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.mail.from:noreply@missanye.com}") String from,
            @Value("${app.mail.fail-closed:false}") boolean failClosed
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.failClosed = failClosed;
    }

    public void sendText(String to, String subject, String body) {
        doSend(to, subject, body);
    }

    @Async("mailExecutor")
    public void sendTextAsync(String to, String subject, String body) {
        try {
            doSend(to, subject, body);
        } catch (BusinessRuleException ex) {
            log.error("E-mail non envoyé (fail-closed) vers {}: {}", maskRecipient(to), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Échec envoi e-mail async vers {}: {}", maskRecipient(to), ex.getMessage());
            if (failClosed) {
                throw new BusinessRuleException(
                        "Service e-mail indisponible. Réessayez plus tard ou contactez le support."
                );
            }
        }
    }

    private void doSend(String to, String subject, String body) {
        if (!StringUtils.hasText(to)) {
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            if (failClosed) {
                throw new BusinessRuleException(
                        "Service e-mail non configuré. Contactez l'administrateur de la plateforme."
                );
            }
            log.info(
                    "[MAIL-DEV] SMTP non configuré — message non expédié vers {} (sujet: {})",
                    maskRecipient(to),
                    subject
            );
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to.trim());
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
        log.info("E-mail envoyé à {}", maskRecipient(to));
    }

    static String maskRecipient(String value) {
        if (!StringUtils.hasText(value)) {
            return "[absent]";
        }
        String normalized = value.trim();
        int at = normalized.indexOf('@');
        if (at <= 0) {
            return normalized.substring(0, Math.min(2, normalized.length())) + "***";
        }
        return normalized.substring(0, 1) + "***" + normalized.substring(at);
    }
}
