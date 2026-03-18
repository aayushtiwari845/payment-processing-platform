package com.paymentplatform.transaction.dto;

import com.paymentplatform.transaction.domain.Transaction;
import com.paymentplatform.transaction.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt,
        Boolean fraudApproved,
        BigDecimal fraudScore,
        String fraudModelVersion,
        String fraudReason,
        Instant fraudDecisionAt,
        Long version
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getSourceAccountId(),
                transaction.getDestinationAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getIdempotencyKey(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                transaction.getFraudApproved(),
                transaction.getFraudScore(),
                transaction.getFraudModelVersion(),
                transaction.getFraudReason(),
                transaction.getFraudDecisionAt(),
                transaction.getVersion()
        );
    }
}
