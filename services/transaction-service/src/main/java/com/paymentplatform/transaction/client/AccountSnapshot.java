package com.paymentplatform.transaction.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountSnapshot(
        UUID id,
        UUID customerId,
        String email,
        String currency,
        BigDecimal balance,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
