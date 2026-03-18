package com.paymentplatform.transaction.service;

import com.paymentplatform.transaction.auth.RequestAuthContext;
import com.paymentplatform.transaction.client.AccountClient;
import com.paymentplatform.transaction.client.AccountSnapshot;
import com.paymentplatform.transaction.domain.Transaction;
import com.paymentplatform.transaction.domain.TransactionStatus;
import com.paymentplatform.transaction.dto.CreateTransactionRequest;
import com.paymentplatform.transaction.dto.FraudDecisionRequest;
import com.paymentplatform.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final RequestAuthContext INTERNAL_AUTH = new RequestAuthContext("internal", java.util.List.of(), true);
    private static final RequestAuthContext CUSTOMER_AUTH = new RequestAuthContext("customer@example.com", java.util.List.of("CUSTOMER"), false);

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionEventPublisher transactionEventPublisher;

    @Mock
    private AccountClient accountClient;

    @Mock
    private TransactionMetrics transactionMetrics;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void shouldReturnExistingTransactionForSameIdempotencyKey() {
        String idempotencyKey = "txn-001";
        CreateTransactionRequest request = buildRequest();
        Transaction existing = new Transaction();
        existing.setId(UUID.randomUUID());
        existing.setStatus(TransactionStatus.PENDING);
        when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

        Transaction result = transactionService.createTransaction(request, idempotencyKey, INTERNAL_AUTH);

        assertThat(result).isSameAs(existing);
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountClient, never()).getAccount(any(UUID.class));
    }

    @Test
    void shouldRejectMissingIdempotencyKey() {
        assertThatThrownBy(() -> transactionService.createTransaction(buildRequest(), " ", INTERNAL_AUTH))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldRejectWhenSourceAccountHasInsufficientBalance() {
        CreateTransactionRequest request = buildRequest();
        stubActiveAccounts(request, BigDecimal.ONE);

        assertThatThrownBy(() -> transactionService.createTransaction(request, "txn-002", INTERNAL_AUTH))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldPersistAndPublishWhenValidationPasses() {
        CreateTransactionRequest request = buildRequest();
        stubActiveAccounts(request, BigDecimal.valueOf(500));
        when(transactionRepository.findByIdempotencyKey("txn-003")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                transaction.setId(UUID.randomUUID());
            }
            return transaction;
        });

        Transaction transaction = transactionService.createTransaction(request, "txn-003", INTERNAL_AUTH);

        assertThat(transaction.getCurrency()).isEqualTo("USD");
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountClient, never()).adjustBalance(any(UUID.class), any(BigDecimal.class), org.mockito.ArgumentMatchers.anyString());
        verify(transactionEventPublisher).publish(transaction);
    }

    @Test
    void shouldAllowCustomerToCreateTransactionForOwnedSourceAccount() {
        CreateTransactionRequest request = buildRequest();
        stubAccounts(request, "customer@example.com", "merchant@example.com", BigDecimal.valueOf(500));
        when(transactionRepository.findByIdempotencyKey("txn-ownership")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                transaction.setId(UUID.randomUUID());
            }
            return transaction;
        });

        Transaction transaction = transactionService.createTransaction(request, "txn-ownership", CUSTOMER_AUTH);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void shouldRejectCustomerCreatingTransactionForAnotherUsersSourceAccount() {
        CreateTransactionRequest request = buildRequest();
        stubAccounts(request, "other@example.com", "merchant@example.com", BigDecimal.valueOf(500));
        when(transactionRepository.findByIdempotencyKey("txn-forbidden")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "txn-forbidden", CUSTOMER_AUTH))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldCompleteTransactionWhenFraudDecisionApproves() {
        Transaction pendingTransaction = buildPendingTransaction();
        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));
        when(accountClient.adjustBalance(any(UUID.class), any(BigDecimal.class), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> new AccountSnapshot(
                        invocation.getArgument(0),
                        UUID.randomUUID(),
                        "adjusted@example.com",
                        "USD",
                        BigDecimal.valueOf(500),
                        "ACTIVE",
                        null,
                        null,
                        0L
                ));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                transaction.setId(UUID.randomUUID());
            }
            return transaction;
        });

        Transaction transaction = transactionService.applyFraudDecision(
                pendingTransaction.getId(),
                new FraudDecisionRequest(true, 0.05, "baseline-rule-model", "approved"),
                INTERNAL_AUTH
        );

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(transaction.getFraudApproved()).isTrue();
        assertThat(transaction.getFraudScore()).isEqualByComparingTo("0.05");
        assertThat(transaction.getFraudModelVersion()).isEqualTo("baseline-rule-model");
        assertThat(transaction.getFraudReason()).isEqualTo("approved");
        assertThat(transaction.getFraudDecisionAt()).isNotNull();
        verify(transactionEventPublisher).publish(transaction);
    }

    @Test
    void shouldRejectTransactionWhenFraudDecisionRejects() {
        Transaction pendingTransaction = buildPendingTransaction();
        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction transaction = transactionService.applyFraudDecision(
                pendingTransaction.getId(),
                new FraudDecisionRequest(false, 0.98, "baseline-rule-model", "high-risk-score"),
                INTERNAL_AUTH
        );

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(transaction.getFraudApproved()).isFalse();
        assertThat(transaction.getFraudScore()).isEqualByComparingTo("0.98");
        assertThat(transaction.getFraudModelVersion()).isEqualTo("baseline-rule-model");
        assertThat(transaction.getFraudReason()).isEqualTo("high-risk-score");
        assertThat(transaction.getFraudDecisionAt()).isNotNull();
        verify(accountClient, never()).adjustBalance(any(UUID.class), any(BigDecimal.class), org.mockito.ArgumentMatchers.anyString());
        verify(transactionEventPublisher).publish(transaction);
    }

    @Test
    void shouldRejectCustomerApplyingFraudDecision() {
        Transaction pendingTransaction = buildPendingTransaction();
        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));

        assertThatThrownBy(() -> transactionService.applyFraudDecision(
                pendingTransaction.getId(),
                new FraudDecisionRequest(true, 0.05, "baseline-rule-model", "approved"),
                CUSTOMER_AUTH
        )).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldCompensateAndRejectWhenSettlementFailsAfterDebit() {
        Transaction pendingTransaction = buildPendingTransaction();
        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountClient.adjustBalance(
                eq(pendingTransaction.getSourceAccountId()),
                eq(pendingTransaction.getAmount().negate()),
                argThat(reason -> reason.startsWith("transaction-debit:"))
        )).thenReturn(accountSnapshotFor(
                pendingTransaction.getSourceAccountId(),
                "customer@example.com",
                BigDecimal.valueOf(100)
        ));
        when(accountClient.adjustBalance(
                eq(pendingTransaction.getDestinationAccountId()),
                eq(pendingTransaction.getAmount()),
                argThat(reason -> reason.startsWith("transaction-credit:"))
        )).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "credit failed"));
        when(accountClient.adjustBalance(
                eq(pendingTransaction.getSourceAccountId()),
                eq(pendingTransaction.getAmount()),
                argThat(reason -> reason.startsWith("transaction-compensation:"))
        )).thenReturn(accountSnapshotFor(
                pendingTransaction.getSourceAccountId(),
                "customer@example.com",
                BigDecimal.valueOf(500)
        ));

        assertThatThrownBy(() -> transactionService.applyFraudDecision(
                pendingTransaction.getId(),
                new FraudDecisionRequest(true, 0.05, "baseline-rule-model", "approved"),
                INTERNAL_AUTH
        )).isInstanceOf(ResponseStatusException.class);
        verify(transactionEventPublisher).publish(any(Transaction.class));
    }

    private CreateTransactionRequest buildRequest() {
        return new CreateTransactionRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.valueOf(25.50),
            "USD"
        );
    }

    private void stubActiveAccounts(CreateTransactionRequest request, BigDecimal sourceBalance) {
        stubAccounts(request, "account@example.com", "destination@example.com", sourceBalance);
    }

    private void stubAccounts(CreateTransactionRequest request, String sourceEmail, String destinationEmail, BigDecimal sourceBalance) {
        when(accountClient.getAccount(request.sourceAccountId()))
                .thenReturn(accountSnapshotFor(request.sourceAccountId(), sourceEmail, sourceBalance));
        when(accountClient.getAccount(request.destinationAccountId()))
                .thenReturn(accountSnapshotFor(request.destinationAccountId(), destinationEmail, BigDecimal.valueOf(500)));
    }

    private Transaction buildPendingTransaction() {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setSourceAccountId(UUID.randomUUID());
        transaction.setDestinationAccountId(UUID.randomUUID());
        transaction.setAmount(BigDecimal.valueOf(25.50));
        transaction.setCurrency("USD");
        transaction.setIdempotencyKey("txn-pending");
        transaction.setStatus(TransactionStatus.PENDING);
        return transaction;
    }

    private AccountSnapshot accountSnapshotFor(UUID accountId, String email, BigDecimal balance) {
        return new AccountSnapshot(
                accountId,
                UUID.randomUUID(),
                email,
                "USD",
                balance,
                "ACTIVE",
                null,
                null,
                0L
        );
    }
}
