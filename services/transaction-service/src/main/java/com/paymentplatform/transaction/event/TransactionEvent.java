package com.paymentplatform.transaction.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionEvent(
        String eventType,
        UUID transactionId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        String status,
        String idempotencyKey,
        Instant occurredAt
) {
}
