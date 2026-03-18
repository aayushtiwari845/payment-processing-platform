package com.paymentplatform.gateway.auth;

import java.util.List;

public record AuthenticatedUser(
        String username,
        List<String> roles
) {
}
