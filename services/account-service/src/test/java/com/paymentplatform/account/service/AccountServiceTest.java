package com.paymentplatform.account.service;

import com.paymentplatform.account.domain.Account;
import com.paymentplatform.account.domain.AccountStatus;
import com.paymentplatform.account.dto.AccountRequest;
import com.paymentplatform.account.repository.AccountRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMetrics accountMetrics;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldCreateAccountWhenEmailIsUnique() {
        AccountRequest request = new AccountRequest(
                UUID.randomUUID(),
                "alice@example.com",
                "USD",
                BigDecimal.TEN,
                AccountStatus.ACTIVE
        );

        when(accountRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = accountService.createAccount(request);

        assertThat(created.getEmail()).isEqualTo("alice@example.com");
        assertThat(created.getCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        AccountRequest request = new AccountRequest(
                UUID.randomUUID(),
                "alice@example.com",
                "USD",
                BigDecimal.ONE,
                AccountStatus.ACTIVE
        );

        when(accountRepository.findByEmail(request.email())).thenReturn(Optional.of(new Account()));

        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldRejectDeleteWhenBalanceIsPositive() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setBalance(BigDecimal.ONE);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.deleteAccount(accountId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldDeleteAccountWhenBalanceIsZero() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setBalance(BigDecimal.ZERO);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        accountService.deleteAccount(accountId);

        verify(accountRepository).delete(account);
    }

    @Test
    void shouldAdjustBalanceWhenSufficientFundsExist() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setBalance(BigDecimal.TEN);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account updated = accountService.adjustBalance(accountId, BigDecimal.valueOf(-4));

        assertThat(updated.getBalance()).isEqualByComparingTo("6");
    }

    @Test
    void shouldRejectBalanceAdjustmentWhenItWouldOverdraw() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setBalance(BigDecimal.valueOf(3));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.adjustBalance(accountId, BigDecimal.valueOf(-5)))
                .isInstanceOf(ResponseStatusException.class);
        verify(accountRepository, never()).save(any(Account.class));
    }
}
