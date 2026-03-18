package com.paymentplatform.account.auth;

import java.util.List;

public record RequestAuthContext(
        String username,
        List<String> roles,
        boolean internalService
) {
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
