package com.paymentplatform.transaction.service;

import com.paymentplatform.transaction.auth.RequestAuthContext;
import com.paymentplatform.transaction.client.AccountClient;
import com.paymentplatform.transaction.client.AccountSnapshot;
import com.paymentplatform.transaction.domain.Transaction;
import com.paymentplatform.transaction.domain.TransactionStatus;
import com.paymentplatform.transaction.dto.CreateTransactionRequest;
import com.paymentplatform.transaction.dto.FraudDecisionRequest;
import com.paymentplatform.transaction.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventPublisher transactionEventPublisher;
    private final AccountClient accountClient;
    private final TransactionMetrics transactionMetrics;
    private final TransactionAuditService transactionAuditService;

    public TransactionService(
            TransactionRepository transactionRepository,
            TransactionEventPublisher transactionEventPublisher,
            AccountClient accountClient,
            TransactionMetrics transactionMetrics,
            TransactionAuditService transactionAuditService
    ) {
        this.transactionRepository = transactionRepository;
        this.transactionEventPublisher = transactionEventPublisher;
        this.accountClient = accountClient;
        this.transactionMetrics = transactionMetrics;
        this.transactionAuditService = transactionAuditService;
    }

    public Transaction getTransaction(UUID transactionId, RequestAuthContext authContext) {
        Transaction transaction = getTransactionById(transactionId);
        authorizeRead(authContext, transaction);
        return transaction;
    }

    public Transaction getTransactionById(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    @Transactional
    public Transaction createTransaction(CreateTransactionRequest request, String idempotencyKey, RequestAuthContext authContext) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            transactionAuditService.logFailure("TRANSACTION_CREATE", null, authContext, "same-account-transfer");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination accounts must differ");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            transactionAuditService.logFailure("TRANSACTION_CREATE", null, authContext, "missing-idempotency-key");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }

        Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTransaction.isPresent()) {
            authorizeRead(authContext, existingTransaction.get());
            return existingTransaction.get();
        }

        AccountSnapshot sourceAccount = accountClient.getAccount(request.sourceAccountId());
        AccountSnapshot destinationAccount = accountClient.getAccount(request.destinationAccountId());
        authorizeCreate(authContext, sourceAccount);
        validateAccounts(request, sourceAccount, destinationAccount);

        Transaction pendingTransaction = createPendingTransaction(request, idempotencyKey);
        transactionEventPublisher.publish(pendingTransaction);
        transactionMetrics.incrementPending();
        transactionAuditService.logSuccess(
                "TRANSACTION_CREATE",
                pendingTransaction.getId(),
                authContext,
                "status=" + pendingTransaction.getStatus()
        );
        return pendingTransaction;
    }

    @Transactional
    public Transaction applyFraudDecision(UUID transactionId, FraudDecisionRequest request, RequestAuthContext authContext) {
        long start = System.nanoTime();
        authorizeFraudDecision(authContext);
        Transaction transaction = getTransactionById(transactionId);
        if (transaction.getStatus() == TransactionStatus.COMPLETED || transaction.getStatus() == TransactionStatus.REJECTED) {
            transactionMetrics.recordFraudDecision(System.nanoTime() - start);
            transactionAuditService.logSuccess(
                    "TRANSACTION_FRAUD_DECISION",
                    transaction.getId(),
                    authContext,
                    "already-final:" + transaction.getStatus()
            );
            return transaction;
        }
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction is not awaiting fraud review");
        }

        applyFraudMetadata(transaction, request);

        if (!request.approved()) {
            transaction.setStatus(TransactionStatus.REJECTED);
            Transaction rejectedTransaction = transactionRepository.save(transaction);
            transactionEventPublisher.publish(rejectedTransaction);
            transactionMetrics.incrementFraudRejected();
            transactionMetrics.incrementRejected();
            transactionMetrics.recordFraudDecision(System.nanoTime() - start);
            transactionAuditService.logSuccess(
                    "TRANSACTION_FRAUD_DECISION",
                    rejectedTransaction.getId(),
                    authContext,
                    "rejected-by-fraud"
            );
            return rejectedTransaction;
        }

        boolean sourceDebited = false;
        try {
            accountClient.adjustBalance(
                    transaction.getSourceAccountId(),
                    transaction.getAmount().negate(),
                    "transaction-debit:" + transaction.getId()
            );
            sourceDebited = true;
            accountClient.adjustBalance(
                    transaction.getDestinationAccountId(),
                    transaction.getAmount(),
                    "transaction-credit:" + transaction.getId()
            );

            transaction.setStatus(TransactionStatus.COMPLETED);
            Transaction completedTransaction = transactionRepository.save(transaction);
            transactionEventPublisher.publish(completedTransaction);
            transactionMetrics.incrementFraudApproved();
            transactionMetrics.incrementCompleted();
            transactionMetrics.recordFraudDecision(System.nanoTime() - start);
            transactionAuditService.logSuccess(
                    "TRANSACTION_FRAUD_DECISION",
                    completedTransaction.getId(),
                    authContext,
                    "approved-and-completed"
            );
            return completedTransaction;
        } catch (RuntimeException exception) {
            transaction.setStatus(TransactionStatus.REJECTED);
            Transaction rejectedTransaction = transactionRepository.save(transaction);

            if (sourceDebited) {
                compensateSourceAccount(transaction);
            }

            transactionEventPublisher.publish(rejectedTransaction);
            transactionMetrics.incrementFraudApproved();
            transactionMetrics.incrementRejected();
            transactionMetrics.recordFraudDecision(System.nanoTime() - start);
            transactionAuditService.logFailure(
                    "TRANSACTION_FRAUD_DECISION",
                    rejectedTransaction.getId(),
                    authContext,
                    "settlement-failed:" + exception.getClass().getSimpleName()
            );
            throw wrapTransactionFailure(exception);
        }
    }

    private Transaction createPendingTransaction(CreateTransactionRequest request, String idempotencyKey) {
        Transaction transaction = new Transaction();
        transaction.setSourceAccountId(request.sourceAccountId());
        transaction.setDestinationAccountId(request.destinationAccountId());
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency().toUpperCase());
        transaction.setIdempotencyKey(idempotencyKey);
        return transactionRepository.save(transaction);
    }

    private void validateAccounts(
            CreateTransactionRequest request,
            AccountSnapshot sourceAccount,
            AccountSnapshot destinationAccount
    ) {
        if (!"ACTIVE".equalsIgnoreCase(sourceAccount.status()) || !"ACTIVE".equalsIgnoreCase(destinationAccount.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both accounts must be active");
        }
        if (!sourceAccount.currency().equalsIgnoreCase(request.currency())
                || !destinationAccount.currency().equalsIgnoreCase(request.currency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction currency must match both accounts");
        }
        if (sourceAccount.balance().compareTo(request.amount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source account has insufficient balance");
        }
    }

    private void authorizeCreate(RequestAuthContext authContext, AccountSnapshot sourceAccount) {
        if (authContext.internalService() || authContext.hasRole("ADMIN")) {
            return;
        }
        if (authContext.hasRole("CUSTOMER") && sourceAccount.email().equalsIgnoreCase(authContext.username())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to create this transaction");
    }

    private void authorizeRead(RequestAuthContext authContext, Transaction transaction) {
        if (authContext.internalService() || authContext.hasRole("ADMIN") || authContext.hasRole("OPS")) {
            return;
        }
        if (authContext.hasRole("CUSTOMER")) {
            AccountSnapshot sourceAccount = accountClient.getAccount(transaction.getSourceAccountId());
            AccountSnapshot destinationAccount = accountClient.getAccount(transaction.getDestinationAccountId());
            if (sourceAccount.email().equalsIgnoreCase(authContext.username())
                    || destinationAccount.email().equalsIgnoreCase(authContext.username())) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this transaction");
    }

    private void authorizeFraudDecision(RequestAuthContext authContext) {
        if (authContext.internalService() || authContext.hasRole("FRAUD_ENGINE") || authContext.hasRole("ADMIN")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to apply fraud decision");
    }

    private void compensateSourceAccount(Transaction transaction) {
        try {
            accountClient.adjustBalance(
                    transaction.getSourceAccountId(),
                    transaction.getAmount(),
                    "transaction-compensation:" + transaction.getId()
            );
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Transaction failed and compensation could not be completed",
                    exception
            );
        }
    }

    private void applyFraudMetadata(Transaction transaction, FraudDecisionRequest request) {
        transaction.setFraudApproved(request.approved());
        transaction.setFraudScore(BigDecimal.valueOf(request.fraudScore()));
        transaction.setFraudModelVersion(request.modelVersion());
        transaction.setFraudReason(request.reason());
        transaction.setFraudDecisionAt(Instant.now());
    }

    private ResponseStatusException wrapTransactionFailure(RuntimeException exception) {
        if (exception instanceof ResponseStatusException responseStatusException) {
            return responseStatusException;
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Transaction processing failed", exception);
    }
}
