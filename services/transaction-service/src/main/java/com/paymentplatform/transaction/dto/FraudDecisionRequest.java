package com.paymentplatform.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FraudDecisionRequest(
        @NotNull Boolean approved,
        @NotNull Double fraudScore,
        @NotBlank String modelVersion,
        @NotBlank String reason
) {
}
