package com.paymentplatform.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GatewaySecurityProperties.class, GatewayRateLimitProperties.class})
public class GatewayConfiguration {
}
