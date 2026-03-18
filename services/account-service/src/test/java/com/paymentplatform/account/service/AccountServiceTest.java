package com.paymentplatform.account.service;

import com.paymentplatform.account.auth.RequestAuthContext;
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

    private static final RequestAuthContext ADMIN_AUTH = new RequestAuthContext("admin", java.util.List.of("ADMIN"), false);
    private static final RequestAuthContext CUSTOMER_AUTH = new RequestAuthContext("customer@example.com", java.util.List.of("CUSTOMER"), false);
    private static final RequestAuthContext INTERNAL_AUTH = new RequestAuthContext("internal", java.util.List.of(), true);

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

        Account created = accountService.createAccount(request, ADMIN_AUTH);

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

        assertThatThrownBy(() -> accountService.createAccount(request, ADMIN_AUTH))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldRejectDeleteWhenBalanceIsPositive() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setBalance(BigDecimal.ONE);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.deleteAccount(accountId, ADMIN_AUTH))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldDeleteAccountWhenBalanceIsZero() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setBalance(BigDecimal.ZERO);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        accountService.deleteAccount(accountId, ADMIN_AUTH);

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

        Account updated = accountService.adjustBalance(accountId, BigDecimal.valueOf(-4), ADMIN_AUTH);

        assertThat(updated.getBalance()).isEqualByComparingTo("6");
    }

    @Test
    void shouldAllowCustomerToReadOwnAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setEmail("customer@example.com");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        Account result = accountService.getAccount(accountId, CUSTOMER_AUTH);

        assertThat(result).isSameAs(account);
    }

    @Test
    void shouldRejectCustomerReadingAnotherUsersAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setEmail("other@example.com");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.getAccount(accountId, CUSTOMER_AUTH))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldAllowInternalServiceToAdjustBalance() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setBalance(BigDecimal.valueOf(15));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account updated = accountService.adjustBalance(accountId, BigDecimal.valueOf(-5), INTERNAL_AUTH);

        assertThat(updated.getBalance()).isEqualByComparingTo("10");
    }

    @Test
    void shouldRejectBalanceAdjustmentWhenItWouldOverdraw() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setBalance(BigDecimal.valueOf(3));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.adjustBalance(accountId, BigDecimal.valueOf(-5), ADMIN_AUTH))
                .isInstanceOf(ResponseStatusException.class);
        verify(accountRepository, never()).save(any(Account.class));
    }
}
