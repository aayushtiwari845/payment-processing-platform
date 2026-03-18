package com.paymentplatform.transaction.controller;

import com.paymentplatform.transaction.auth.AuthContextFactory;
import com.paymentplatform.transaction.auth.RequestAuthContext;
import com.paymentplatform.transaction.dto.CreateTransactionRequest;
import com.paymentplatform.transaction.dto.FraudDecisionRequest;
import com.paymentplatform.transaction.dto.TransactionResponse;
import com.paymentplatform.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthContextFactory authContextFactory;

    public TransactionController(TransactionService transactionService, AuthContextFactory authContextFactory) {
        this.transactionService = transactionService;
        this.authContextFactory = authContextFactory;
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse getTransaction(
            @PathVariable UUID transactionId,
            @RequestHeader(name = "X-Authenticated-User", required = false) String username,
            @RequestHeader(name = "X-Authenticated-Roles", required = false) String roles,
            @RequestHeader(name = "X-Internal-Service-Token", required = false) String internalToken
    ) {
        RequestAuthContext authContext = authContextFactory.fromHeaders(username, roles, internalToken);
        return TransactionResponse.from(transactionService.getTransaction(transactionId, authContext));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(name = "X-Authenticated-User", required = false) String username,
            @RequestHeader(name = "X-Authenticated-Roles", required = false) String roles,
            @RequestHeader(name = "X-Internal-Service-Token", required = false) String internalToken
    ) {
        RequestAuthContext authContext = authContextFactory.fromHeaders(username, roles, internalToken);
        return TransactionResponse.from(transactionService.createTransaction(request, idempotencyKey, authContext));
    }

    @PostMapping("/{transactionId}/fraud-decision")
    public TransactionResponse applyFraudDecision(
            @PathVariable UUID transactionId,
            @Valid @RequestBody FraudDecisionRequest request,
            @RequestHeader(name = "X-Authenticated-User", required = false) String username,
            @RequestHeader(name = "X-Authenticated-Roles", required = false) String roles,
            @RequestHeader(name = "X-Internal-Service-Token", required = false) String internalToken
    ) {
        RequestAuthContext authContext = authContextFactory.fromHeaders(username, roles, internalToken);
        return TransactionResponse.from(transactionService.applyFraudDecision(transactionId, request, authContext));
    }
}
