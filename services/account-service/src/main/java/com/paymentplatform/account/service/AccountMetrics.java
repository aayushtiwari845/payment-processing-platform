package com.paymentplatform.account.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AccountMetrics {

    private final Counter createdCounter;
    private final Counter updatedCounter;
    private final Counter deletedCounter;
    private final Counter balanceAdjustmentCounter;
    private final Counter rejectedBalanceAdjustmentCounter;
    private final Timer accountLookupTimer;

    public AccountMetrics(MeterRegistry meterRegistry) {
        this.createdCounter = meterRegistry.counter("payment_account_created_total");
        this.updatedCounter = meterRegistry.counter("payment_account_updated_total");
        this.deletedCounter = meterRegistry.counter("payment_account_deleted_total");
        this.balanceAdjustmentCounter = meterRegistry.counter("payment_account_balance_adjustment_total");
        this.rejectedBalanceAdjustmentCounter = meterRegistry.counter("payment_account_balance_adjustment_rejected_total");
        this.accountLookupTimer = meterRegistry.timer("payment_account_lookup_duration");
    }

    public void incrementCreated() {
        createdCounter.increment();
    }

    public void incrementUpdated() {
        updatedCounter.increment();
    }

    public void incrementDeleted() {
        deletedCounter.increment();
    }

    public void incrementBalanceAdjustment() {
        balanceAdjustmentCounter.increment();
    }

    public void incrementRejectedBalanceAdjustment() {
        rejectedBalanceAdjustmentCounter.increment();
    }

    public void recordLookup(long nanos) {
        accountLookupTimer.record(nanos, TimeUnit.NANOSECONDS);
    }
}
