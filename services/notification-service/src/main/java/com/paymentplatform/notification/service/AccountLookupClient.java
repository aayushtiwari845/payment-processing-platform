package com.paymentplatform.notification.service;

import com.paymentplatform.notification.domain.AccountSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class AccountLookupClient {

    private final RestClient restClient;

    public AccountLookupClient(@Value("${clients.account-service.base-url}") String accountServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(accountServiceBaseUrl)
                .build();
    }

    public AccountSnapshot getAccount(UUID accountId) {
        try {
            AccountSnapshot account = restClient.get()
                    .uri("/api/v1/accounts/{accountId}", accountId)
                    .retrieve()
                    .body(AccountSnapshot.class);

            if (account == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account service returned empty response");
            }

            return account;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account lookup failed", exception);
        }
    }
}
