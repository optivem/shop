package com.mycompany.myshop.config;

import com.mycompany.myshop.core.services.external.ResilienceSettings;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

// Binds resilience.* (timeouts, retry, circuit breaker for the outbound gateways) into ResilienceSettings.
@Configuration
@EnableConfigurationProperties(ResilienceSettings.class)
public class ResilienceConfig {
}
