package com.paymentplatform.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitKeyResolverTest {

    private final RateLimitKeyResolver rateLimitKeyResolver = new RateLimitKeyResolver();

    @Test
    void shouldPreferAuthenticatedUserHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/transactions")
                        .header("X-Authenticated-User", "Customer@example.com")
                        .build()
        );

        String key = rateLimitKeyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("user:customer@example.com");
    }

    @Test
    void shouldFallbackToForwardedForHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/accounts")
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.1")
                        .build()
        );

        String key = rateLimitKeyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("ip:203.0.113.10");
    }

    @Test
    void shouldFallbackToUserAgentHashWhenIdentityIsMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/accounts")
                        .header(HttpHeaders.USER_AGENT, "Codex-Test-Agent")
                        .build()
        );

        String key = rateLimitKeyResolver.resolve(exchange).block();

        assertThat(key).startsWith("agent:");
    }
}
