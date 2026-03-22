package com.paymentplatform.gateway.security;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component("rateLimitKeyResolver")
public class RateLimitKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(org.springframework.web.server.ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String authenticatedUser = request.getHeaders().getFirst("X-Authenticated-User");
        if (StringUtils.hasText(authenticatedUser)) {
            return Mono.just("user:" + authenticatedUser.toLowerCase());
        }

        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String clientIp = forwardedFor.split(",")[0].trim();
            return Mono.just("ip:" + clientIp);
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return Mono.just("ip:" + remoteAddress.getAddress().getHostAddress());
        }

        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        if (StringUtils.hasText(userAgent)) {
            return Mono.just("agent:" + Integer.toHexString(userAgent.hashCode()));
        }

        return Mono.just("anonymous");
    }
}
