package com.paymentplatform.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class RouteAuthorizationService {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/token",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus"
    );

    public boolean isPublic(ServerHttpRequest request) {
        return PUBLIC_PATHS.contains(request.getPath().value());
    }

    public boolean isAllowed(ServerHttpRequest request, List<String> roles) {
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();
        if (method == null) {
            return false;
        }

        if (path.startsWith("/api/v1/accounts")) {
            if (method == HttpMethod.GET) {
                return hasAnyRole(roles, "ADMIN", "OPS");
            }
            return hasAnyRole(roles, "ADMIN");
        }

        if (path.matches("^/api/v1/transactions/[^/]+/fraud-decision$")) {
            return method == HttpMethod.POST && hasAnyRole(roles, "FRAUD_ENGINE", "ADMIN");
        }

        if (path.startsWith("/api/v1/transactions")) {
            if (method == HttpMethod.GET) {
                return hasAnyRole(roles, "ADMIN", "OPS", "CUSTOMER");
            }
            if (method == HttpMethod.POST) {
                return hasAnyRole(roles, "ADMIN", "CUSTOMER");
            }
        }

        return false;
    }

    private boolean hasAnyRole(List<String> roles, String... expectedRoles) {
        for (String expectedRole : expectedRoles) {
            if (roles.contains(expectedRole)) {
                return true;
            }
        }
        return false;
    }
}
