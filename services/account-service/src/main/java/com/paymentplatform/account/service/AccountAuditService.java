package com.paymentplatform.account.service;

import com.paymentplatform.account.auth.RequestAuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccountAuditService {

    private static final Logger log = LoggerFactory.getLogger(AccountAuditService.class);

    public void logSuccess(String action, UUID accountId, RequestAuthContext authContext, String detail) {
        log("success", action, accountId, authContext, detail);
    }

    public void logFailure(String action, UUID accountId, RequestAuthContext authContext, String detail) {
        log("failure", action, accountId, authContext, detail);
    }

    private void log(String outcome, String action, UUID accountId, RequestAuthContext authContext, String detail) {
        log.info(
                "audit service=account outcome={} action={} actor={} roles={} internalService={} accountId={} detail={}",
                outcome,
                action,
                authContext.username(),
                String.join(",", authContext.roles()),
                authContext.internalService(),
                accountId,
                detail
        );
    }
}
