package com.paymentplatform.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BalanceAdjustmentRequest(
        @NotNull
        @Digits(integer = 15, fraction = 4)
        @DecimalMin(value = "-999999999999999.9999", inclusive = true)
        BigDecimal amountDelta,
        @NotBlank String reason
) {
}
