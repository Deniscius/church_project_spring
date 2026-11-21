package com.eyram.dev.church_project_spring;

import static org.assertj.core.api.Assertions.*;

import com.eyram.dev.church_project_spring.config.SasPayProperties;
import com.eyram.dev.church_project_spring.controller.SasPayWebhookController;
import com.eyram.dev.church_project_spring.service.billing.SubscriptionBillingService;
import com.eyram.dev.church_project_spring.service.payment.saspay.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "app.admin-seed.enabled=false", "app.tenant-catalog.backfill-on-startup=false",
      "app.demande.unpaid-auto-cancel-enabled=false", "app.demande.unpaid-reminder-enabled=false",
      "app.demande.celebration-reminder-enabled=false",
          "app.demande.celebration-auto-complete-enabled=false",
      "saspay.poll-delay-ms=3600000", "spring.flyway.repair-on-migrate=false"
    })
@Transactional
@EnabledIfEnvironmentVariable(named = "CI_POSTGRES_SMOKE", matches = "true")
class SasPaySettlementIntegrationTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired EntityManager entities;
  @Autowired ObjectMapper json;
  @Autowired SasPayCheckoutStore store;
  @Autowired SasPayPaymentService service;
  @Autowired SubscriptionBillingService billing;
  @Autowired TransactionTemplate transactions;
  long parish;
  UUID parishPublicId;
  byte[] signingKey;
  SasPayWebhookController controller;

  @BeforeEach
  void fixtures() {
    parishPublicId = UUID.randomUUID();
    parish =
        jdbc.queryForObject(
            """
            INSERT INTO paroisse(public_id,nom,adresse,doyenne_id,statut_tenant,is_active,email)
            SELECT ?,?,'Fictif',id,'ACTIVE',true,'fixture@missanye.com'
            FROM doyenne ORDER BY id LIMIT 1 RETURNING id
            """,
            Long.class,
            parishPublicId,
            "SasPay fixture " + parishPublicId);
    var properties = new SasPayProperties();
    properties.setEnabled(true);
    properties.setWebhookSecret(UUID.randomUUID().toString());
    signingKey = properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8);
    controller =
        new SasPayWebhookController(
            new SasPayWebhookVerifier(properties), store, json, transactions);
  }

  private SasPayCheckoutStore.Checkout checkout(String purpose, UUID target, int fee) {
    UUID id = UUID.randomUUID(), remote = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO saspay_checkout(id,purpose,target_id,amount,platform_fee,remote_id,state)
        VALUES (?,?,?,2500,?,?,'PENDING')
        """,
        id,
        purpose,
        target,
        fee,
        remote);
    return store.get(id, false);
  }

  private SasPayCheckoutStore.Checkout demand() {
    long type =
        jdbc.queryForObject(
            """
            INSERT INTO type_demande(public_id,paroisse_id,type_principal,libelle)
            VALUES (?,?,'EUCHARISTIE','Fictif') RETURNING id
            """,
            Long.class,
            UUID.randomUUID(),
            parish);
    long price =
        jdbc.queryForObject(
            """
            INSERT INTO forfait_tarif(public_id,type_demande_id,code_forfait,nom_forfait,
                nature_forfait,montant_forfait,nombre_celebration)
            VALUES (?,?,?,'Fictif','NORMALE',2400,1) RETURNING id
            """,
            Long.class,
            UUID.randomUUID(),
            type,
            UUID.randomUUID().toString());
    long mode =
        jdbc.queryForObject(
            "SELECT id FROM type_paiement WHERE mode='TMONEY' AND status_del=false LIMIT 1",
            Long.class);
    long demand =
        jdbc.queryForObject(
            """
INSERT INTO demande(public_id,code_suivie,paroisse_id,type_demande_id,forfait_tarif_id,
    type_paiement_id,nom_fidele,prenom_fidele,tel_fidele,intention,montant,
    statut_demande,statut_paiement,statut_validation)
VALUES (?,?,?,?,?,?,'Fictif','Test','+22890000000','Fictif',2400,'VALIDEE','EN_ATTENTE','VALIDEE') RETURNING id
""",
            Long.class,
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            parish,
            type,
            price,
            mode);
    UUID invoice = UUID.randomUUID();
    long invoiceId =
        jdbc.queryForObject(
            """
            INSERT INTO facture(public_id,demande_id,ref_facture,montant,statut_paiement)
            VALUES (?,?,?,2400,'EN_ATTENTE') RETURNING id
            """,
            Long.class,
            invoice,
            demand,
            UUID.randomUUID().toString());
    var session = checkout("DEMANDE", invoice, 100);
    jdbc.update(
        """
        INSERT INTO details_paiement(public_id,facture_id,type_paiement_id,montant,provider,
            id_transaction,statut_paiement,date_details_paiement)
        VALUES (?,?,?,2400,'SASPAY',?,'EN_ATTENTE',CURRENT_TIMESTAMP)
        """,
        UUID.randomUUID(),
        invoiceId,
        mode,
        session.reference());
    return session;
  }

  private ObjectNode payment(UUID transaction) {
    return json.createObjectNode()
        .put("id", transaction.toString())
        .put("status", "SUCCESS")
        .put("currency", "XOF")
        .put("transaction_type", "PAIEMENT")
        .put("flow_direction", "INBOUND")
        .put("fee_charge_mode", "ADD_ON")
        .put("requested_amount", "2500.00")
        .put("debited_amount", "2537.50")
        .put("net_amount", "2500.00")
        .put("client_fee", "37.50");
  }

  private byte[] event(UUID transaction) throws Exception {
    return json.writeValueAsBytes(
        json.createObjectNode()
            .put("event", "transaction.success")
            .set("data", json.createObjectNode().put("id", transaction.toString())));
  }

  private String sign(byte[] body, String timestamp) throws Exception {
    var mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
    mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(mac.doFinal(body));
  }

  private void deliver(UUID transaction) throws Exception {
    byte[] body = event(transaction);
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    assertThat(controller.receive(body, sign(body, timestamp), timestamp, "transaction.success"))
        .containsEntry("received", true);
  }

  private long credits() {
    entities.flush();
    return jdbc.queryForObject(
        "SELECT count(*) FROM ecriture_comptable WHERE paroisse_id=?", Long.class, parish);
  }

  @Test
  void duplicateEventAndRepeatedVerificationCreditExactlyOnce() throws Exception {
    var session = demand();
    UUID transaction = UUID.randomUUID();
    deliver(transaction);
    deliver(transaction);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM saspay_webhook_event WHERE transaction_id=?",
                Long.class,
                transaction))
        .isEqualTo(1);
    assertThat(credits()).isZero(); // Notification alone is never a settlement.
    var verified = payment(transaction);
    service.confirm(session, verified);
    deliver(transaction);
    service.confirm(session, verified); // The caller may still hold the original PENDING snapshot.
    assertThat(credits()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT solde_disponible FROM compte_paroisse WHERE paroisse_id=?",
                Integer.class,
                parish))
        .isEqualTo(2400);
    assertThat(store.get(session.id(), false).state()).isEqualTo("SUCCESS");
    assertThat(
            jdbc.queryForObject(
                "SELECT statut_paiement FROM facture WHERE public_id=?",
                String.class,
                session.target()))
        .isEqualTo("PAYE");
  }

  @Test
  void duplicateNotificationAndVerificationNeverExtendSubscriptionTwice() throws Exception {
    var session = checkout("ABONNEMENT", UUID.randomUUID(), 0);
    jdbc.update(
        """
INSERT INTO paroisse_abonnement(public_id,paroisse_id,plan,montant,duree_mois,statut,id_transaction)
VALUES (?,?,'MENSUEL',2500,1,'EN_ATTENTE',?)
""",
        session.target(),
        parish,
        session.reference());
    UUID transaction = UUID.randomUUID();
    deliver(transaction);
    service.confirm(session, payment(transaction));
    entities.flush();
    var expiry =
        jdbc.queryForObject(
            "SELECT subscription_expires_at FROM paroisse WHERE id=?", LocalDateTime.class, parish);
    assertThat(expiry).isAfter(LocalDateTime.now());
    deliver(transaction);
    service.confirm(session, payment(transaction));
    billing.activateFromProviderTransaction(session.reference());
    entities.flush();
    assertThat(
            jdbc.queryForObject(
                "SELECT subscription_expires_at FROM paroisse WHERE id=?",
                LocalDateTime.class,
                parish))
        .isEqualTo(expiry);
    assertThat(
            jdbc.queryForObject(
                "SELECT fin_at FROM paroisse_abonnement WHERE public_id=?",
                LocalDateTime.class,
                session.target()))
        .isEqualTo(expiry);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM paroisse_abonnement WHERE paroisse_id=? AND statut='ACTIF'",
                Long.class,
                parish))
        .isEqualTo(1);
    assertThat(credits()).isZero();
  }

  @Test
  void amountOrNetMismatchStaysUnderReviewWithoutCredit() {
    var session = demand();
    for (String field : new String[] {"requested_amount", "net_amount"}) {
      service.confirm(session, payment(UUID.randomUUID()).put(field, "2499.00"));
      assertThat(store.get(session.id(), false).state()).isEqualTo("REVIEW");
    }
    assertThat(credits()).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT statut_paiement FROM facture WHERE public_id=?",
                String.class,
                session.target()))
        .isEqualTo("EN_ATTENTE");
  }

  @Test
  void badSignatureAndExpiredTimestampCannotPersistAnyEvent() throws Exception {
    UUID transaction = UUID.randomUUID();
    byte[] body = event(transaction);
    String now = Long.toString(Instant.now().getEpochSecond());
    assertThatThrownBy(() -> controller.receive(body, "0".repeat(64), now, "transaction.success"))
        .isInstanceOf(ResponseStatusException.class);
    String old = Long.toString(Instant.now().getEpochSecond() - 600);
    assertThatThrownBy(() -> controller.receive(body, sign(body, old), old, "transaction.success"))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM saspay_webhook_event WHERE transaction_id=?",
                Long.class,
                transaction))
        .isZero();
    assertThat(credits()).isZero();
  }
}
