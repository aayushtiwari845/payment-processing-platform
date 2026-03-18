package com.paymentplatform.notification.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class NotificationMetrics {

    private final Counter processedCounter;
    private final Counter sentCounter;
    private final Counter retryCounter;
    private final Counter failedCounter;
    private final Timer deliveryTimer;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.processedCounter = meterRegistry.counter("payment_notification_processed_total");
        this.sentCounter = meterRegistry.counter("payment_notification_sent_total");
        this.retryCounter = meterRegistry.counter("payment_notification_retry_total");
        this.failedCounter = meterRegistry.counter("payment_notification_failed_total");
        this.deliveryTimer = meterRegistry.timer("payment_notification_delivery_duration");
    }

    public void incrementProcessed() {
        processedCounter.increment();
    }

    public void incrementSent() {
        sentCounter.increment();
    }

    public void incrementRetry() {
        retryCounter.increment();
    }

    public void incrementFailed() {
        failedCounter.increment();
    }

    public void recordDelivery(long nanos) {
        deliveryTimer.record(nanos, TimeUnit.NANOSECONDS);
    }
}
