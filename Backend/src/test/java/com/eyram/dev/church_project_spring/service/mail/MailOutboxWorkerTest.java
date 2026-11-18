package com.eyram.dev.church_project_spring.service.mail;

import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class MailOutboxWorkerTest {
  @Test
  @SuppressWarnings("unchecked")
  void smtpFailureRetainsRetryAndDoesNotPreventNextDelivery() throws Exception {
    var outbox = mock(MailOutbox.class);
    ObjectProvider<JavaMailSender> providers = mock(ObjectProvider.class);
    var sender = mock(JavaMailSender.class);
    when(providers.getIfAvailable()).thenReturn(sender);
    var first = new MailOutbox.Claimed(UUID.randomUUID(), new byte[0], 1, UUID.randomUUID());
    var second = new MailOutbox.Claimed(UUID.randomUUID(), new byte[0], 1, UUID.randomUUID());
    when(outbox.claim()).thenReturn(List.of(first, second));
    when(outbox.decode(first))
        .thenReturn(new MailOutbox.Message("one@example.test", "Subject", "Body"));
    when(outbox.decode(second))
        .thenReturn(new MailOutbox.Message("two@example.test", "Subject", "Body"));
    doThrow(new MailSendException("provider unavailable"))
        .doNothing()
        .when(sender)
        .send(any(SimpleMailMessage.class));
    new MailOutboxWorker(outbox, providers).deliver();
    verify(outbox).failed(eq(first), any(MailSendException.class));
    verify(outbox, never()).sent(first);
    verify(outbox).sent(second);
  }
}
