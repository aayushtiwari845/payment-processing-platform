package com.paymentplatform.notification.service;

import com.paymentplatform.notification.domain.AccountSnapshot;
import com.paymentplatform.notification.domain.TransactionEvent;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final AccountLookupClient accountLookupClient;
    private final EmailSender emailSender;
    private final NotificationMetrics notificationMetrics;

    public NotificationService(
            AccountLookupClient accountLookupClient,
            EmailSender emailSender,
            NotificationMetrics notificationMetrics
    ) {
        this.accountLookupClient = accountLookupClient;
        this.emailSender = emailSender;
        this.notificationMetrics = notificationMetrics;
    }

    public void handleTransactionEvent(TransactionEvent event) {
        if (!"COMPLETED".equalsIgnoreCase(event.status())) {
            return;
        }

        long start = System.nanoTime();
        AccountSnapshot recipientAccount = accountLookupClient.getAccount(event.destinationAccountId());
        String subject = "Payment received";
        String htmlBody = """
                <html>
                  <body>
                    <h2>Transaction completed</h2>
                    <p>Transaction ID: %s</p>
                    <p>Amount: %s %s</p>
                  </body>
                </html>
                """.formatted(event.transactionId(), event.amount(), event.currency());

        notificationMetrics.incrementProcessed();
        sendWithRetry(recipientAccount.email(), subject, htmlBody);
        notificationMetrics.recordDelivery(System.nanoTime() - start);
    }

    private void sendWithRetry(String recipient, String subject, String htmlBody) {
        long[] backoffMillis = {250L, 500L, 1000L};
        RuntimeException lastFailure = null;

        for (int attempt = 0; attempt < backoffMillis.length; attempt++) {
            try {
                emailSender.send(recipient, subject, htmlBody);
                notificationMetrics.incrementSent();
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                notificationMetrics.incrementRetry();
                if (attempt == backoffMillis.length - 1) {
                    break;
                }
                try {
                    Thread.sleep(backoffMillis[attempt]);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Notification retry interrupted", interruptedException);
                }
            }
        }

        notificationMetrics.incrementFailed();
        throw new IllegalStateException("Notification delivery failed after retries", lastFailure);
    }
}
