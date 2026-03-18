package com.paymentplatform.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String recipient, String subject, String htmlBody) {
        log.info("Mock email sent to={} subject={} body={}", recipient, subject, htmlBody);
    }
}
