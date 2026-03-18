package com.paymentplatform.transaction.client;

import java.math.BigDecimal;

public record BalanceAdjustmentCommand(
        BigDecimal amountDelta,
        String reason
) {
}
