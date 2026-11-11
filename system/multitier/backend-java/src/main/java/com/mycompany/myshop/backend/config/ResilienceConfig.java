package com.mycompany.myshop.backend.config;

import com.mycompany.myshop.backend.core.services.external.ResilienceSettings;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

// Binds resilience.* (timeouts, retry, circuit breaker for the outbound gateways) into ResilienceSettings.
@Configuration
@EnableConfigurationProperties(ResilienceSettings.class)
public class ResilienceConfig {
}
