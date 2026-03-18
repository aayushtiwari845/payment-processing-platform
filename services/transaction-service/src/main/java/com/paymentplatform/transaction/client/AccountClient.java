package com.paymentplatform.transaction.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(@Value("${clients.account-service.base-url}") String accountServiceBaseUrl) {
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
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account " + accountId + " not found", exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account service unavailable", exception);
        }
    }

    public AccountSnapshot adjustBalance(UUID accountId, BigDecimal amountDelta, String reason) {
        try {
            AccountSnapshot account = restClient.post()
                    .uri("/api/v1/accounts/{accountId}/balance-adjustments", accountId)
                    .body(new BalanceAdjustmentCommand(amountDelta, reason))
                    .retrieve()
                    .body(AccountSnapshot.class);

            if (account == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account service returned empty response");
            }

            return account;
        } catch (HttpClientErrorException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account balance adjustment failed", exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account service unavailable", exception);
        }
    }
}
