package com.paymentplatform.account.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class AuthContextFactory {

    private final String internalServiceToken;

    public AuthContextFactory(@Value("${security.internal-token}") String internalServiceToken) {
        this.internalServiceToken = internalServiceToken;
    }

    public RequestAuthContext fromHeaders(String username, String rolesHeader, String providedInternalToken) {
        boolean internal = internalServiceToken.equals(providedInternalToken);
        List<String> roles = rolesHeader == null || rolesHeader.isBlank()
                ? List.of()
                : Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        return new RequestAuthContext(username, roles, internal);
    }
}
