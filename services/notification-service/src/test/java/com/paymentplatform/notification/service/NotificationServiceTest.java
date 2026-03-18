package com.paymentplatform.notification.service;

import com.paymentplatform.notification.domain.AccountSnapshot;
import com.paymentplatform.notification.domain.TransactionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private AccountLookupClient accountLookupClient;

    @Mock
    private EmailSender emailSender;

    @Mock
    private NotificationMetrics notificationMetrics;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldIgnoreNonCompletedTransactions() {
        notificationService.handleTransactionEvent(buildEvent("REJECTED"));

        verifyNoInteractions(accountLookupClient, emailSender);
    }

    @Test
    void shouldRetryEmailUntilSuccess() {
        TransactionEvent event = buildEvent("COMPLETED");
        when(accountLookupClient.getAccount(event.destinationAccountId())).thenReturn(new AccountSnapshot(
                event.destinationAccountId(),
                UUID.randomUUID(),
                "recipient@example.com",
                "USD",
                BigDecimal.ZERO,
                "ACTIVE",
                Instant.now(),
                Instant.now(),
                0L
        ));

        org.mockito.Mockito.doThrow(new IllegalStateException("smtp down"))
                .doThrow(new IllegalStateException("smtp still down"))
                .doNothing()
                .when(emailSender)
                .send(org.mockito.ArgumentMatchers.eq("recipient@example.com"), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        notificationService.handleTransactionEvent(event);

        verify(emailSender, times(3)).send(
                org.mockito.ArgumentMatchers.eq("recipient@example.com"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private TransactionEvent buildEvent(String status) {
        return new TransactionEvent(
                "TRANSACTION_" + status,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(25.50),
                "USD",
                status,
                "idempotency-1",
                Instant.now()
        );
    }
}
