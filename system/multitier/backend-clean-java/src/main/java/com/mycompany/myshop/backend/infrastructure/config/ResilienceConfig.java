package com.mycompany.myshop.backend.infrastructure.config;

import com.mycompany.myshop.backend.infrastructure.external.ResilienceSettings;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

// Binds resilience.* (timeouts, retry, circuit breaker for the outbound gateways) into ResilienceSettings.
@Configuration
@EnableConfigurationProperties(ResilienceSettings.class)
public class ResilienceConfig {
}
