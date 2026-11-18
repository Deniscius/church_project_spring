package com.eyram.dev.church_project_spring.service.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailOutboxWorker {
  private final MailOutbox outbox;
  private final ObjectProvider<JavaMailSender> senders;

  @Value("${app.mail.from:noreply@missanye.com}")
  private String from;

  @Scheduled(
      fixedDelayString = "${app.mail.outbox-delay-ms:10000}",
      scheduler = "mailOutboxScheduler")
  public void deliver() {
    var sender = senders.getIfAvailable();
    if (sender == null) return;
    for (var mail : outbox.claim()) {
      try {
        var content = outbox.decode(mail);
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(content.to());
        message.setSubject(content.subject());
        message.setText(content.body());
        sender.send(message);
        outbox.sent(mail);
      } catch (Exception failure) {
        outbox.failed(mail, failure);
        // No recipient, token, SMTP response or body in logs.
        log.warn(
            "E-mail différé non envoyé: id={}, attempt={}, type={}",
            mail.id(),
            mail.attempts(),
            failure.getClass().getSimpleName());
      }
    }
  }

  @Scheduled(fixedDelay = 3600000, initialDelay = 60000, scheduler = "mailOutboxScheduler")
  public void maintain() {
    outbox.maintain();
  }
}
