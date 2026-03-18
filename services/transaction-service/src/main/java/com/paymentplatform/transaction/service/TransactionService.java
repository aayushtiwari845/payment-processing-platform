package com.paymentplatform.transaction.service;

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

    public TransactionService(
            TransactionRepository transactionRepository,
            TransactionEventPublisher transactionEventPublisher,
            AccountClient accountClient,
            TransactionMetrics transactionMetrics
    ) {
        this.transactionRepository = transactionRepository;
        this.transactionEventPublisher = transactionEventPublisher;
        this.accountClient = accountClient;
        this.transactionMetrics = transactionMetrics;
    }

    public Transaction getTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    @Transactional
    public Transaction createTransaction(CreateTransactionRequest request, String idempotencyKey) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination accounts must differ");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }

        Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTransaction.isPresent()) {
            return existingTransaction.get();
        }

        AccountSnapshot sourceAccount = accountClient.getAccount(request.sourceAccountId());
        AccountSnapshot destinationAccount = accountClient.getAccount(request.destinationAccountId());
        validateAccounts(request, sourceAccount, destinationAccount);

        Transaction pendingTransaction = createPendingTransaction(request, idempotencyKey);
        transactionEventPublisher.publish(pendingTransaction);
        transactionMetrics.incrementPending();
        return pendingTransaction;
    }

    @Transactional
    public Transaction applyFraudDecision(UUID transactionId, FraudDecisionRequest request) {
        long start = System.nanoTime();
        Transaction transaction = getTransaction(transactionId);
        if (transaction.getStatus() == TransactionStatus.COMPLETED || transaction.getStatus() == TransactionStatus.REJECTED) {
            transactionMetrics.recordFraudDecision(System.nanoTime() - start);
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
