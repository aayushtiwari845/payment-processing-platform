package com.paymentplatform.account.service;

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

    public AccountService(AccountRepository accountRepository, AccountMetrics accountMetrics) {
        this.accountRepository = accountRepository;
        this.accountMetrics = accountMetrics;
    }

    public List<Account> listAccounts() {
        return accountRepository.findAll();
    }

    @Cacheable(cacheNames = "accounts", key = "#accountId")
    public Account getAccount(UUID accountId) {
        long start = System.nanoTime();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        accountMetrics.recordLookup(System.nanoTime() - start);
        return account;
    }

    public Account createAccount(AccountRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        accountRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
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
        return savedAccount;
    }

    @CachePut(cacheNames = "accounts", key = "#accountId")
    public Account updateAccount(UUID accountId, AccountRequest request) {
        Account account = getAccount(accountId);
        String normalizedEmail = request.email().trim().toLowerCase();
        if (accountRepository.existsByEmailAndIdNot(normalizedEmail, accountId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account email already exists");
        }

        account.setCustomerId(request.customerId());
        account.setEmail(normalizedEmail);
        account.setCurrency(request.currency().toUpperCase());
        account.setBalance(request.balance());
        account.setStatus(request.status());
        Account savedAccount = accountRepository.save(account);
        accountMetrics.incrementUpdated();
        return savedAccount;
    }

    @Transactional
    @CachePut(cacheNames = "accounts", key = "#accountId")
    public Account adjustBalance(UUID accountId, BigDecimal amountDelta) {
        if (amountDelta.compareTo(BigDecimal.ZERO) == 0) {
            accountMetrics.incrementRejectedBalanceAdjustment();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Balance adjustment cannot be zero");
        }

        Account account = getAccount(accountId);
        BigDecimal updatedBalance = account.getBalance().add(amountDelta);
        if (updatedBalance.compareTo(BigDecimal.ZERO) < 0) {
            accountMetrics.incrementRejectedBalanceAdjustment();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds for balance adjustment");
        }

        account.setBalance(updatedBalance);
        Account savedAccount = accountRepository.save(account);
        accountMetrics.incrementBalanceAdjustment();
        return savedAccount;
    }

    @CacheEvict(cacheNames = "accounts", key = "#accountId")
    public void deleteAccount(UUID accountId) {
        Account account = getAccount(accountId);
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account with positive balance cannot be deleted");
        }
        accountRepository.delete(account);
        accountMetrics.incrementDeleted();
    }
}
