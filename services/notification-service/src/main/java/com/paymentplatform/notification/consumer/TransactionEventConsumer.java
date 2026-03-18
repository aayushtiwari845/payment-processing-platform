package com.paymentplatform.notification.consumer;

import com.paymentplatform.notification.domain.TransactionEvent;
import com.paymentplatform.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventConsumer {

    private final NotificationService notificationService;

    public TransactionEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "transaction-events", groupId = "${spring.kafka.consumer.group-id}")
    public void onTransactionEvent(TransactionEvent event) {
        notificationService.handleTransactionEvent(event);
    }
}
