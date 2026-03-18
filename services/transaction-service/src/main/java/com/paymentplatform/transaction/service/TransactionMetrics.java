package com.paymentplatform.transaction.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TransactionMetrics {

    private final Counter pendingCounter;
    private final Counter completedCounter;
    private final Counter rejectedCounter;
    private final Counter fraudApprovedCounter;
    private final Counter fraudRejectedCounter;
    private final Timer fraudDecisionTimer;

    public TransactionMetrics(MeterRegistry meterRegistry) {
        this.pendingCounter = meterRegistry.counter("payment_transaction_pending_total");
        this.completedCounter = meterRegistry.counter("payment_transaction_completed_total");
        this.rejectedCounter = meterRegistry.counter("payment_transaction_rejected_total");
        this.fraudApprovedCounter = meterRegistry.counter("payment_fraud_decision_approved_total");
        this.fraudRejectedCounter = meterRegistry.counter("payment_fraud_decision_rejected_total");
        this.fraudDecisionTimer = meterRegistry.timer("payment_transaction_fraud_decision_duration");
    }

    public void incrementPending() {
        pendingCounter.increment();
    }

    public void incrementCompleted() {
        completedCounter.increment();
    }

    public void incrementRejected() {
        rejectedCounter.increment();
    }

    public void incrementFraudApproved() {
        fraudApprovedCounter.increment();
    }

    public void incrementFraudRejected() {
        fraudRejectedCounter.increment();
    }

    public void recordFraudDecision(long nanos) {
        fraudDecisionTimer.record(nanos, TimeUnit.NANOSECONDS);
    }
}
