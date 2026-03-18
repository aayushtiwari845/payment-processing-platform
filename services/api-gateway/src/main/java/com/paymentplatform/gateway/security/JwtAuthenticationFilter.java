package com.paymentplatform.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.gateway.auth.AuthenticatedUser;
import com.paymentplatform.gateway.auth.JwtService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final RouteAuthorizationService routeAuthorizationService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            RouteAuthorizationService routeAuthorizationService,
            ObjectMapper objectMapper
    ) {
        this.jwtService = jwtService;
        this.routeAuthorizationService = routeAuthorizationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (routeAuthorizationService.isPublic(request)) {
            return chain.filter(exchange);
        }

        String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return writeError(exchange.getResponse(), HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        AuthenticatedUser authenticatedUser;
        try {
            authenticatedUser = jwtService.parseToken(authorizationHeader.substring(7));
        } catch (RuntimeException exception) {
            return writeError(exchange.getResponse(), HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        if (!routeAuthorizationService.isAllowed(request, authenticatedUser.roles())) {
            return writeError(exchange.getResponse(), HttpStatus.FORBIDDEN, "Access denied");
        }

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Authenticated-User", authenticatedUser.username())
                .header("X-Authenticated-Roles", String.join(",", authenticatedUser.roles()))
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> writeError(ServerHttpResponse response, HttpStatus status, String message) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(Map.of(
                    "status", status.value(),
                    "error", status.getReasonPhrase(),
                    "message", message
            ));
        } catch (JsonProcessingException exception) {
            body = ("{\"status\":" + status.value() + ",\"message\":\"" + message + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }
}
