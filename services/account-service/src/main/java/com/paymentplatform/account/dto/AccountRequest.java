package com.paymentplatform.account.dto;

import com.paymentplatform.account.domain.AccountStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountRequest(
        @NotNull UUID customerId,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull @DecimalMin("0.00") BigDecimal balance,
        @NotNull AccountStatus status
) {
}
