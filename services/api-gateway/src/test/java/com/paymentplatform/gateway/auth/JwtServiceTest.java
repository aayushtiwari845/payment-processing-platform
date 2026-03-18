package com.paymentplatform.gateway.auth;

import com.paymentplatform.gateway.config.GatewaySecurityProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void shouldRoundTripJwtClaims() {
        GatewaySecurityProperties properties = new GatewaySecurityProperties();
        GatewaySecurityProperties.Jwt jwt = new GatewaySecurityProperties.Jwt();
        jwt.setSecret("change-me-change-me-change-me-change-me-1234567890");
        jwt.setIssuer("payment-platform-gateway");
        jwt.setExpirationMinutes(60);
        properties.setJwt(jwt);

        JwtService jwtService = new JwtService(properties);

        AuthResponse authResponse = jwtService.issueToken("customer", List.of("CUSTOMER"));
        AuthenticatedUser authenticatedUser = jwtService.parseToken(authResponse.accessToken());

        assertThat(authenticatedUser.username()).isEqualTo("customer");
        assertThat(authenticatedUser.roles()).containsExactly("CUSTOMER");
    }
}
