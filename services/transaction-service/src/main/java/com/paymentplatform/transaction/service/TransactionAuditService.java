package com.paymentplatform.transaction.service;

import com.paymentplatform.transaction.auth.RequestAuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TransactionAuditService {

    private static final Logger log = LoggerFactory.getLogger(TransactionAuditService.class);

    public void logSuccess(String action, UUID transactionId, RequestAuthContext authContext, String detail) {
        log("success", action, transactionId, authContext, detail);
    }

    public void logFailure(String action, UUID transactionId, RequestAuthContext authContext, String detail) {
        log("failure", action, transactionId, authContext, detail);
    }

    private void log(String outcome, String action, UUID transactionId, RequestAuthContext authContext, String detail) {
        log.info(
                "audit service=transaction outcome={} action={} actor={} roles={} internalService={} transactionId={} detail={}",
                outcome,
                action,
                authContext.username(),
                String.join(",", authContext.roles()),
                authContext.internalService(),
                transactionId,
                detail
        );
    }
}
