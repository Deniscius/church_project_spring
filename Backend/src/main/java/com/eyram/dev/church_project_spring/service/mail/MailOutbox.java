package com.eyram.dev.church_project_spring.service.mail;

import com.eyram.dev.church_project_spring.config.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Enqueue in the caller's transaction; SMTP is never part of that transaction. */
@Service
public class MailOutbox {
  public record Message(String to, String subject, String body) {}

  public record Claimed(UUID id, byte[] payload, int attempts, UUID token) {}

  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public MailOutbox(
      JdbcTemplate jdbc,
      ObjectMapper json,
      JwtProperties jwt,
      @Value("${app.mail.outbox-key:}") String configuredKey) {
    this.jdbc = jdbc;
    this.json = json;
    try {
      String secret = configuredKey.isBlank() ? jwt.secret() : configuredKey;
      if (secret == null || secret.isBlank())
        throw new IllegalStateException("Clé de chiffrement e-mail absente");
      key =
          new SecretKeySpec(
              MessageDigest.getInstance("SHA-256")
                  .digest(("missanye-mail-outbox-v1:" + secret).getBytes(StandardCharsets.UTF_8)),
              "AES");
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional
  public void enqueue(String to, String subject, String body) {
    if (to == null || to.isBlank()) return;
    UUID id = UUID.randomUUID();
    try {
      byte[] nonce = new byte[12];
      random.nextBytes(nonce);
      Cipher cipher = cipher(Cipher.ENCRYPT_MODE, id, nonce);
      byte[] encrypted =
          cipher.doFinal(json.writeValueAsBytes(new Message(to.trim(), subject, body)));
      byte[] payload =
          ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array();
      jdbc.update("INSERT INTO mail_outbox(id,payload) VALUES (?,?)", id, payload);
    } catch (Exception e) {
      throw new IllegalStateException("Impossible de conserver l'e-mail à envoyer", e);
    }
  }

  public Message decode(Claimed mail) throws Exception {
    var buffer = ByteBuffer.wrap(mail.payload());
    byte[] nonce = new byte[12];
    buffer.get(nonce);
    byte[] encrypted = new byte[buffer.remaining()];
    buffer.get(encrypted);
    return json.readValue(
        cipher(Cipher.DECRYPT_MODE, mail.id(), nonce).doFinal(encrypted), Message.class);
  }

  private Cipher cipher(int mode, UUID id, byte[] nonce) throws GeneralSecurityException {
    var cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(mode, key, new GCMParameterSpec(128, nonce));
    cipher.updateAAD(id.toString().getBytes(StandardCharsets.UTF_8));
    return cipher;
  }

  @Transactional
  public List<Claimed> claim() {
    UUID token = UUID.randomUUID();
    return jdbc.query(
        """
        WITH ready AS (
          SELECT id FROM mail_outbox
          WHERE attempts<8 AND created_at>CURRENT_TIMESTAMP-INTERVAL '24 hours'
            AND ((state='PENDING' AND available_at<=CURRENT_TIMESTAMP)
              OR (state='SENDING' AND lease_until<CURRENT_TIMESTAMP))
          ORDER BY available_at,id LIMIT 5 FOR UPDATE SKIP LOCKED
        )
        UPDATE mail_outbox m SET state='SENDING',attempts=m.attempts+1,
          lease_token=?,lease_until=CURRENT_TIMESTAMP+INTERVAL '10 minutes'
        FROM ready r WHERE m.id=r.id RETURNING m.id,m.payload,m.attempts
        """,
        (r, n) -> new Claimed(r.getObject(1, UUID.class), r.getBytes(2), r.getInt(3), token),
        token);
  }

  public void sent(Claimed mail) {
    jdbc.update(
        "UPDATE mail_outbox SET"
            + " state='SENT',payload=NULL,finished_at=CURRENT_TIMESTAMP,lease_token=NULL,lease_until=NULL"
            + " WHERE id=? AND lease_token=? AND state='SENDING'",
        mail.id(),
        mail.token());
  }

  public void failed(Claimed mail, Exception failure) {
    long seconds = Math.min(3600, 30L * (1L << Math.min(mail.attempts(), 7)));
    jdbc.update(
        """
UPDATE mail_outbox SET state=CASE WHEN attempts>=8 THEN 'DEAD' ELSE 'PENDING' END,
 finished_at=CASE WHEN attempts>=8 THEN CURRENT_TIMESTAMP ELSE NULL END,
 available_at=CURRENT_TIMESTAMP+(? * INTERVAL '1 second'),last_error=?,lease_token=NULL,lease_until=NULL
 WHERE id=? AND lease_token=? AND state='SENDING'
""",
        seconds,
        failure.getClass().getSimpleName(),
        mail.id(),
        mail.token());
  }

  public void maintain() {
    jdbc.update(
        "UPDATE mail_outbox SET"
            + " state='DEAD',payload=NULL,finished_at=CURRENT_TIMESTAMP,last_error='Expired' WHERE"
            + " (state='PENDING' AND created_at<CURRENT_TIMESTAMP-INTERVAL '24 hours') OR"
            + " (state='SENDING' AND lease_until<CURRENT_TIMESTAMP AND (attempts>=8 OR"
            + " created_at<CURRENT_TIMESTAMP-INTERVAL '24 hours'))");
    jdbc.update(
        "DELETE FROM mail_outbox WHERE state IN ('SENT','DEAD') AND"
            + " finished_at<CURRENT_TIMESTAMP-INTERVAL '7 days'");
  }
}
