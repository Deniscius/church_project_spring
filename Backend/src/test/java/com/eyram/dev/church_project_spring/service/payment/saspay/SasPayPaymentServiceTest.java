package com.eyram.dev.church_project_spring.service.payment.saspay;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class SasPayPaymentServiceTest {
  @Mock SasPayCheckoutStore store;
  @Mock SasPayClient client;
  @Mock TransactionTemplate transactions;
  @Mock com.eyram.dev.church_project_spring.service.accounting.ParishLedgerService ledger;
  @InjectMocks SasPayPaymentService service;
  final ObjectMapper json = new ObjectMapper();
  SasPayCheckoutStore.Checkout session =
      new SasPayCheckoutStore.Checkout(
          UUID.randomUUID(),
          "DEMANDE",
          UUID.randomUUID(),
          2500,
          100,
          UUID.randomUUID().toString(),
          null,
          "https://pay.saspay.me/checkout/test",
          "PENDING",
          null);

  JsonNode payment() throws Exception {
    return json.readTree(
        "{\"id\":\""
            + UUID.randomUUID()
            + "\",\"status\":\"SUCCESS\",\"currency\":\"XOF\",\"transaction_type\":\"PAIEMENT\",\"flow_direction\":\"INBOUND\",\"fee_charge_mode\":\"ADD_ON\",\"requested_amount\":\"2500.00\",\"debited_amount\":\"2537.50\",\"client_fee\":\"37.50\",\"net_amount\":\"2500.00\"}");
  }

  @Test
  void pendingDoesNotDeliver() throws Exception {
    var data = (com.fasterxml.jackson.databind.node.ObjectNode) payment();
    data.put("status", "PENDING");
    service.confirm(session, data);
    verifyNoInteractions(store, ledger, transactions);
  }

  @Test
  void incorrectAmountNetOrFeeModeRequiresReviewAndCannotCredit() throws Exception {
    doAnswer(
            i -> {
              ((java.util.function.Consumer<org.springframework.transaction.TransactionStatus>)
                      i.getArgument(0))
                  .accept(mock(org.springframework.transaction.TransactionStatus.class));
              return null;
            })
        .when(transactions)
        .executeWithoutResult(any());
    for (String field :
        java.util.List.of("requested_amount", "net_amount", "debited_amount", "fee_charge_mode")) {
      var data = (com.fasterxml.jackson.databind.node.ObjectNode) payment();
      data.put(field, field.equals("fee_charge_mode") ? "DEDUCTED" : "2499.00");
      service.confirm(session, data);
    }
    verify(store, times(4)).state(session.id(), "REVIEW");
    verifyNoInteractions(ledger);
  }

  @Test
  void foreignCurrencyAndOutboundFundsCannotPayAnInvoice() throws Exception {
    for (String field : java.util.List.of("currency", "flow_direction", "transaction_type")) {
      var data = (com.fasterxml.jackson.databind.node.ObjectNode) payment();
      data.put(field, "WRONG");
      assertThatThrownBy(() -> service.confirm(session, data))
          .isInstanceOf(
              com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException.class);
    }
    verifyNoInteractions(ledger);
  }

  @Test
  void verifiedTransactionMustMatchTheSessionStatusReference() throws Exception {
    String expected = UUID.randomUUID().toString();
    when(client.status(session.remoteId()))
        .thenReturn(
            json.createObjectNode()
                .put("id", session.remoteId())
                .put("status", "PAID")
                .put("transaction_id", expected));
    when(client.verify(expected)).thenReturn(payment());
    assertThatThrownBy(() -> service.refresh(session))
        .isInstanceOf(
            com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException.class);
    verifyNoInteractions(ledger);
  }

  @Test
  void failedCancelledOrPendingStatusNeverDeliver() throws Exception {
    for (String state : java.util.List.of("FAILED", "CANCELLED", "PENDING")) {
      var data = (com.fasterxml.jackson.databind.node.ObjectNode) payment();
      data.put("status", state);
      service.confirm(session, data);
    }
    verifyNoInteractions(store, ledger, transactions);
  }

  @Test
  void paidHistoricalInvoiceRemainsReadableWithoutAnyProviderCallOrRecomputedAmounts() {
    var demands = mock(com.eyram.dev.church_project_spring.repositories.DemandeRepository.class);
    var invoices = mock(com.eyram.dev.church_project_spring.repositories.FactureRepository.class);
    var payments =
        mock(com.eyram.dev.church_project_spring.repositories.DetailsPaiementRepository.class);
    var local =
        new SasPayPaymentService(
            client,
            store,
            new com.eyram.dev.church_project_spring.config.SasPayProperties(),
            new com.eyram.dev.church_project_spring.config.PlatformBillingProperties(),
            demands,
            invoices,
            payments,
            null,
            null,
            null,
            null,
            ledger,
            null,
            transactions);
    String code = "MS-TEST-ABCDEFGHIJ";
    var demand = new com.eyram.dev.church_project_spring.entities.Demande();
    demand.setPublicId(UUID.randomUUID());
    demand.setCodeSuivie(code);
    demand.setStatutPaiement(com.eyram.dev.church_project_spring.enums.StatutPaiementEnum.PAYE);
    var mode = new com.eyram.dev.church_project_spring.entities.TypePaiement();
    mode.setMode(com.eyram.dev.church_project_spring.enums.ModePaiement.TMONEY);
    demand.setTypePaiement(mode);
    var invoice = new com.eyram.dev.church_project_spring.entities.Facture();
    invoice.setPublicId(UUID.randomUUID());
    invoice.setDemande(demand);
    invoice.setMontant(2400);
    invoice.setStatutPaiement(com.eyram.dev.church_project_spring.enums.StatutPaiementEnum.PAYE);
    var detail = new com.eyram.dev.church_project_spring.entities.DetailsPaiement();
    detail.setProvider("FEDAPAY"); // Historical provider labels intentionally remain readable.
    detail.setIdTransaction("historical-reference");
    detail.setMontantCharge(2642);
    detail.setMontantFrais(242);
    detail.setMontantFraisAgregeateur(200);
    detail.setMontantFraisPlateforme(42);
    detail.setMontantNet(2200);
    when(demands.findByCodeSuivieWithAssociations(code)).thenReturn(java.util.Optional.of(demand));
    when(invoices.findByDemandePublicIdAndStatusDelFalse(demand.getPublicId()))
        .thenReturn(java.util.Optional.of(invoice));
    when(payments.findByFacturePublicIdAndStatusDelFalse(invoice.getPublicId()))
        .thenReturn(java.util.Optional.of(detail));
    when(transactions.execute(any()))
        .thenAnswer(
            i ->
                ((org.springframework.transaction.support.TransactionCallback<?>) i.getArgument(0))
                    .doInTransaction(
                        mock(org.springframework.transaction.TransactionStatus.class)));
    var snapshot = local.snapshot(code);
    assertThat(snapshot.onlinePaymentRequired()).isFalse();
    assertThat(snapshot.providerTransactionId()).isEqualTo("historical-reference");
    assertThat(snapshot.montantFraisPlateforme()).isEqualTo(42);
    assertThat(snapshot.montantCharge()).isEqualTo(2642);
    assertThat(snapshot.montantNetParoisse()).isEqualTo(2200);
    assertThat(snapshot.montantNetPlateforme()).isEqualTo(42);
    verifyNoInteractions(client, ledger);
  }
}
