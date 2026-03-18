package com.paymentplatform.gateway.security;

import com.paymentplatform.gateway.auth.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "ACCOUNT_SERVICE_URL=http://localhost:65530",
                "TRANSACTION_SERVICE_URL=http://localhost:65531",
                "security.jwt.secret=change-me-change-me-change-me-change-me-1234567890",
                "security.jwt.issuer=payment-platform-gateway-test",
                "security.jwt.expiration-minutes=60"
        }
)
@AutoConfigureWebTestClient
class GatewayAuthIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldIssueTokenForValidCredentials() {
        webTestClient.post()
                .uri("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "username": "customer@example.com",
                          "password": "customer123"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.username").isEqualTo("customer@example.com")
                .jsonPath("$.roles[0]").isEqualTo("CUSTOMER");
    }

    @Test
    void shouldRejectProtectedRouteWithoutToken() {
        webTestClient.get()
                .uri("/api/v1/accounts/123")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Missing or invalid Authorization header");
    }

    @Test
    void shouldRejectCustomerOnFraudDecisionRoute() {
        String token = issueToken("customer@example.com", "customer123");

        webTestClient.post()
                .uri("/api/v1/transactions/123/fraud-decision")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "approved": true,
                          "fraudScore": 0.02,
                          "modelVersion": "test-model",
                          "reason": "approved"
                        }
                        """)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Access denied");
    }

    @Test
    void shouldRejectCustomerOnAccountMutationRoute() {
        String token = issueToken("customer@example.com", "customer123");

        webTestClient.put()
                .uri("/api/v1/accounts/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "customerId": "123e4567-e89b-12d3-a456-426614174000",
                          "email": "customer@example.com",
                          "currency": "USD",
                          "balance": 10.00,
                          "status": "ACTIVE"
                        }
                        """)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Access denied");
    }

    private String issueToken(String username, String password) {
        AuthResponse authResponse = webTestClient.post()
                .uri("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
        return authResponse.accessToken();
    }
}
