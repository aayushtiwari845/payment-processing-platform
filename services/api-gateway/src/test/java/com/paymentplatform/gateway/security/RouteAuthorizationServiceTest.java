package com.paymentplatform.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RouteAuthorizationServiceTest {

    private final RouteAuthorizationService routeAuthorizationService = new RouteAuthorizationService();

    @Test
    void shouldAllowFraudEngineOnFraudDecisionRoute() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/transactions/123/fraud-decision").build();

        boolean allowed = routeAuthorizationService.isAllowed(request, List.of("FRAUD_ENGINE"));

        assertThat(allowed).isTrue();
    }

    @Test
    void shouldDenyCustomerOnAccountMutationRoute() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.PUT, "/api/v1/accounts/123").build();

        boolean allowed = routeAuthorizationService.isAllowed(request, List.of("CUSTOMER"));

        assertThat(allowed).isFalse();
    }
}
