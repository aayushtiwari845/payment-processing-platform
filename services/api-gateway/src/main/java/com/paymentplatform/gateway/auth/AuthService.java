package com.paymentplatform.gateway.auth;

import com.paymentplatform.gateway.config.GatewaySecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final GatewaySecurityProperties properties;
    private final JwtService jwtService;

    public AuthService(GatewaySecurityProperties properties, JwtService jwtService) {
        this.properties = properties;
        this.jwtService = jwtService;
    }

    public AuthResponse authenticate(AuthRequest request) {
        GatewaySecurityProperties.User matchedUser = properties.getUsers().stream()
                .filter(user -> user.getUsername().equals(request.username()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!matchedUser.getPassword().equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return jwtService.issueToken(matchedUser.getUsername(), matchedUser.getRoles());
    }
}
