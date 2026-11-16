package com.eyram.dev.church_project_spring.service.payment.saspay;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

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
                            ((java.util.function.Consumer<
                                                    org.springframework.transaction
                                                            .TransactionStatus>)
                                            i.getArgument(0))
                                    .accept(
                                            mock(
                                                    org.springframework.transaction
                                                            .TransactionStatus.class));
                            return null;
                        })
                .when(transactions)
                .executeWithoutResult(any());
        for (String field :
                java.util.List.of(
                        "requested_amount", "net_amount", "debited_amount", "fee_charge_mode")) {
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
                            com.eyram.dev.church_project_spring.utils.exception
                                    .BusinessRuleException.class);
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
                        com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException
                                .class);
        verifyNoInteractions(ledger);
    }
}
