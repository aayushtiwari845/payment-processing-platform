package com.paymentplatform.transaction.client;

import com.paymentplatform.transaction.logging.CorrelationIdFilter;
import org.slf4j.MDC;
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
    private final String internalServiceToken;

    public AccountClient(
            @Value("${clients.account-service.base-url}") String accountServiceBaseUrl,
            @Value("${clients.account-service.internal-token}") String internalServiceToken
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(accountServiceBaseUrl)
                .build();
        this.internalServiceToken = internalServiceToken;
    }

    public AccountSnapshot getAccount(UUID accountId) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri("/api/v1/accounts/{accountId}", accountId)
                    .header("X-Internal-Service-Token", internalServiceToken);
            addCorrelationId(request);
            AccountSnapshot account = request.retrieve().body(AccountSnapshot.class);

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
            RestClient.RequestBodySpec request = restClient.post()
                    .uri("/api/v1/accounts/{accountId}/balance-adjustments", accountId)
                    .header("X-Internal-Service-Token", internalServiceToken);
            addCorrelationId(request);
            AccountSnapshot account = request
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

    private void addCorrelationId(RestClient.RequestHeadersSpec<?> request) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        if (correlationId != null && !correlationId.isBlank()) {
            request.header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
        }
    }
}
