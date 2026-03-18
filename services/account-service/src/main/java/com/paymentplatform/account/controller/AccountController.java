package com.paymentplatform.account.controller;

import com.paymentplatform.account.auth.AuthContextFactory;
import com.paymentplatform.account.auth.RequestAuthContext;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AuthContextFactory authContextFactory;

    public AccountController(AccountService accountService, AuthContextFactory authContextFactory) {
        this.accountService = accountService;
        this.authContextFactory = authContextFactory;
    }

    @GetMapping
    public List<AccountResponse> listAccounts(
            @RequestHeader(name = "X-Authenticated-User", required = false) String username,
            @RequestHeader(name = "X-Authenticated-Roles", required = false) String roles,
            @RequestHeader(name = "X-Internal-Service-Token", required = false) String internalToken
    ) {
        RequestAuthContext authContext = authContextFactory.fromHeaders(username, roles, internalToken);
        return accountService.listAccounts(authContext).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(
            @PathVariable UUID accountId,
            @RequestHeader(name = "X-Authenticated-User", required = false) String username,
            @RequestHeader(name = "X-Authenticated-Roles", required = false) String roles,
            @RequestHeader(name = "X-Internal-Service-Token", required = false) String internalToken
    ) {
        RequestAuthContext authContext = authContextFactory.fromHeaders(username, roles, internalToken);
        return AccountResponse.from(accountService.getAccount(accountId, authContext));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(
            @Valid @RequestBody AccountRequest request,
            @RequestHeader(name = "X-Authenticated-User", required = false) String username,
            @RequestHeader(name = "X-Authenticated-Roles", required = false) String roles,
            @RequestHeader(name = "X-Internal-Service-Token", required = false) String internalToken
    ) {
        RequestAuthContext authContext = authContextFactory.fromHeaders(username, roles, internalToken);
        return AccountResponse.from(accountService.createAccount(request, authContext));
    }

    @PutMapping("/{accountId}")
    public AccountResponse updateAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody AccountRequest request,
            @RequestHeader(name = "X-Authenticated-User", required = false) String username,
            @RequestHeader(name = "X-Authenticated-Roles", required = false) String roles,
            @RequestHeader(name = "X-Internal-Service-Token", required = false) String internalToken
    ) {
        RequestAuthContext authContext = authContextFactory.fromHeaders(username, roles, internalToken);
        return AccountResponse.from(accountService.updateAccount(accountId, request, authContext));
    }

    @PostMapping("/{accountId}/balance-adjustments")
    public AccountResponse adjustBalance(
            @PathVariable UUID accountId,
            @Valid @RequestBody BalanceAdjustmentRequest request,
            @RequestHeader(name = "X-Authenticated-User", required = false) String username,
            @RequestHeader(name = "X-Authenticated-Roles", required = false) String roles,
            @RequestHeader(name = "X-Internal-Service-Token", required = false) String internalToken
    ) {
        RequestAuthContext authContext = authContextFactory.fromHeaders(username, roles, internalToken);
        return AccountResponse.from(accountService.adjustBalance(accountId, request.amountDelta(), authContext));
    }

    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(
            @PathVariable UUID accountId,
            @RequestHeader(name = "X-Authenticated-User", required = false) String username,
            @RequestHeader(name = "X-Authenticated-Roles", required = false) String roles,
            @RequestHeader(name = "X-Internal-Service-Token", required = false) String internalToken
    ) {
        RequestAuthContext authContext = authContextFactory.fromHeaders(username, roles, internalToken);
        accountService.deleteAccount(accountId, authContext);
    }
}
