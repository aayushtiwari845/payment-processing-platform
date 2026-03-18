package com.paymentplatform.account.dto;

import com.paymentplatform.account.domain.Account;
import com.paymentplatform.account.domain.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID customerId,
        String email,
        String currency,
        BigDecimal balance,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getEmail(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                account.getVersion()
        );
    }
}
