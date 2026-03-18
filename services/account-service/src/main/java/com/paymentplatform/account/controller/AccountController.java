package com.paymentplatform.account.controller;

import com.paymentplatform.account.dto.AccountRequest;
import com.paymentplatform.account.dto.AccountResponse;
import com.paymentplatform.account.dto.BalanceAdjustmentRequest;
import com.paymentplatform.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountResponse> listAccounts() {
        return accountService.listAccounts().stream()
                .map(AccountResponse::from)
                .toList();
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(@PathVariable UUID accountId) {
        return AccountResponse.from(accountService.getAccount(accountId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody AccountRequest request) {
        return AccountResponse.from(accountService.createAccount(request));
    }

    @PutMapping("/{accountId}")
    public AccountResponse updateAccount(@PathVariable UUID accountId, @Valid @RequestBody AccountRequest request) {
        return AccountResponse.from(accountService.updateAccount(accountId, request));
    }

    @PostMapping("/{accountId}/balance-adjustments")
    public AccountResponse adjustBalance(
            @PathVariable UUID accountId,
            @Valid @RequestBody BalanceAdjustmentRequest request
    ) {
        return AccountResponse.from(accountService.adjustBalance(accountId, request.amountDelta()));
    }

    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable UUID accountId) {
        accountService.deleteAccount(accountId);
    }
}
