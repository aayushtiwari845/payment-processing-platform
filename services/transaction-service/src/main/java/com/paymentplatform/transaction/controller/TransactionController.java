package com.paymentplatform.transaction.controller;

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

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse getTransaction(@PathVariable UUID transactionId) {
        return TransactionResponse.from(transactionService.getTransaction(transactionId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return TransactionResponse.from(transactionService.createTransaction(request, idempotencyKey));
    }

    @PostMapping("/{transactionId}/fraud-decision")
    public TransactionResponse applyFraudDecision(
            @PathVariable UUID transactionId,
            @Valid @RequestBody FraudDecisionRequest request
    ) {
        return TransactionResponse.from(transactionService.applyFraudDecision(transactionId, request));
    }
}
