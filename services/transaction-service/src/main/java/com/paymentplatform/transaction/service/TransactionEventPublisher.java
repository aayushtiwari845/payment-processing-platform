package com.paymentplatform.transaction.service;

import com.paymentplatform.transaction.domain.Transaction;
import com.paymentplatform.transaction.event.TransactionEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TransactionEventPublisher {

    private static final String TOPIC = "transaction-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Transaction transaction) {
        TransactionEvent payload = new TransactionEvent(
                "TRANSACTION_" + transaction.getStatus().name(),
                transaction.getId(),
                transaction.getSourceAccountId(),
                transaction.getDestinationAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus().name(),
                transaction.getIdempotencyKey(),
                Instant.now()
        );
        kafkaTemplate.send(TOPIC, transaction.getId().toString(), payload);
    }
}
