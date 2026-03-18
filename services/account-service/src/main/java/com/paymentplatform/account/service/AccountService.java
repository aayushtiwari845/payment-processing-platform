package com.paymentplatform.account.service;

import com.paymentplatform.account.auth.RequestAuthContext;
import com.paymentplatform.account.domain.Account;
import com.paymentplatform.account.dto.AccountRequest;
import com.paymentplatform.account.repository.AccountRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMetrics accountMetrics;
    private final AccountAuditService accountAuditService;

    public AccountService(
            AccountRepository accountRepository,
            AccountMetrics accountMetrics,
            AccountAuditService accountAuditService
    ) {
        this.accountRepository = accountRepository;
        this.accountMetrics = accountMetrics;
        this.accountAuditService = accountAuditService;
    }

    public List<Account> listAccounts(RequestAuthContext authContext) {
        authorizeList(authContext);
        return accountRepository.findAll();
    }

    public Account getAccount(UUID accountId, RequestAuthContext authContext) {
        Account account = getAccountById(accountId);
        authorizeRead(authContext, account);
        return account;
    }

    @Cacheable(cacheNames = "accounts", key = "#accountId")
    public Account getAccountById(UUID accountId) {
        long start = System.nanoTime();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        accountMetrics.recordLookup(System.nanoTime() - start);
        return account;
    }

    public Account createAccount(AccountRequest request, RequestAuthContext authContext) {
        authorizeMutation(authContext);
        String normalizedEmail = request.email().trim().toLowerCase();
        accountRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            accountAuditService.logFailure("ACCOUNT_CREATE", null, authContext, "duplicate-email:" + normalizedEmail);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account email already exists");
        });

        Account account = new Account();
        account.setCustomerId(request.customerId());
        account.setEmail(normalizedEmail);
        account.setCurrency(request.currency().toUpperCase());
        account.setBalance(request.balance());
        account.setStatus(request.status());
        Account savedAccount = accountRepository.save(account);
        accountMetrics.incrementCreated();
        accountAuditService.logSuccess("ACCOUNT_CREATE", savedAccount.getId(), authContext, "email=" + savedAccount.getEmail());
        return savedAccount;
    }

    @CachePut(cacheNames = "accounts", key = "#accountId")
    public Account updateAccount(UUID accountId, AccountRequest request, RequestAuthContext authContext) {
        authorizeMutation(authContext);
        Account account = getAccountById(accountId);
        String normalizedEmail = request.email().trim().toLowerCase();
        if (accountRepository.existsByEmailAndIdNot(normalizedEmail, accountId)) {
            accountAuditService.logFailure("ACCOUNT_UPDATE", accountId, authContext, "duplicate-email:" + normalizedEmail);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account email already exists");
        }

        account.setCustomerId(request.customerId());
        account.setEmail(normalizedEmail);
        account.setCurrency(request.currency().toUpperCase());
        account.setBalance(request.balance());
        account.setStatus(request.status());
        Account savedAccount = accountRepository.save(account);
        accountMetrics.incrementUpdated();
        accountAuditService.logSuccess("ACCOUNT_UPDATE", savedAccount.getId(), authContext, "status=" + savedAccount.getStatus());
        return savedAccount;
    }

    @Transactional
    @CachePut(cacheNames = "accounts", key = "#accountId")
    public Account adjustBalance(UUID accountId, BigDecimal amountDelta, RequestAuthContext authContext) {
        authorizeMutation(authContext);
        if (amountDelta.compareTo(BigDecimal.ZERO) == 0) {
            accountMetrics.incrementRejectedBalanceAdjustment();
            accountAuditService.logFailure("ACCOUNT_ADJUST_BALANCE", accountId, authContext, "zero-delta");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Balance adjustment cannot be zero");
        }

        Account account = getAccountById(accountId);
        BigDecimal updatedBalance = account.getBalance().add(amountDelta);
        if (updatedBalance.compareTo(BigDecimal.ZERO) < 0) {
            accountMetrics.incrementRejectedBalanceAdjustment();
            accountAuditService.logFailure("ACCOUNT_ADJUST_BALANCE", accountId, authContext, "insufficient-funds");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds for balance adjustment");
        }

        account.setBalance(updatedBalance);
        Account savedAccount = accountRepository.save(account);
        accountMetrics.incrementBalanceAdjustment();
        accountAuditService.logSuccess(
                "ACCOUNT_ADJUST_BALANCE",
                savedAccount.getId(),
                authContext,
                "delta=" + amountDelta + ",balance=" + savedAccount.getBalance()
        );
        return savedAccount;
    }

    @CacheEvict(cacheNames = "accounts", key = "#accountId")
    public void deleteAccount(UUID accountId, RequestAuthContext authContext) {
        authorizeMutation(authContext);
        Account account = getAccountById(accountId);
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            accountAuditService.logFailure("ACCOUNT_DELETE", accountId, authContext, "positive-balance");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account with positive balance cannot be deleted");
        }
        accountRepository.delete(account);
        accountMetrics.incrementDeleted();
        accountAuditService.logSuccess("ACCOUNT_DELETE", accountId, authContext, "deleted");
    }

    private void authorizeList(RequestAuthContext authContext) {
        if (authContext.internalService() || authContext.hasRole("ADMIN") || authContext.hasRole("OPS")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to list accounts");
    }

    private void authorizeRead(RequestAuthContext authContext, Account account) {
        if (authContext.internalService() || authContext.hasRole("ADMIN") || authContext.hasRole("OPS")) {
            return;
        }
        if (authContext.hasRole("CUSTOMER") && account.getEmail().equalsIgnoreCase(authContext.username())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this account");
    }

    private void authorizeMutation(RequestAuthContext authContext) {
        if (authContext.internalService() || authContext.hasRole("ADMIN")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to modify accounts");
    }
}
