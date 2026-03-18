package com.paymentplatform.notification.service;

public interface EmailSender {
    void send(String recipient, String subject, String htmlBody);
}
