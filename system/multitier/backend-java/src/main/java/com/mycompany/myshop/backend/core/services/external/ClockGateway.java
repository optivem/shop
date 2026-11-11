package com.mycompany.myshop.backend.core.services.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycompany.myshop.backend.core.dtos.external.ClockGetTimeResponse;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ClockGateway {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final String externalSystemMode;
    private final String clockUrl;
    private final ObservationRegistry observationRegistry;
    private final GatewayHttpClient http;

    public ClockGateway(String externalSystemMode, String clockUrl) {
        this(externalSystemMode, clockUrl, ResilienceSettings.DEFAULTS);
    }

    public ClockGateway(String externalSystemMode, String clockUrl, ResilienceSettings resilience) {
        this(externalSystemMode, clockUrl, ObservationRegistry.NOOP, resilience);
    }

    // The registry gives each outbound call a client span and a W3C traceparent header.
    @Autowired
    public ClockGateway(
            @Value("${external.system-mode}") String externalSystemMode,
            @Value("${clock.url}") String clockUrl,
            ObservationRegistry observationRegistry,
            ResilienceSettings resilience) {
        this.externalSystemMode = externalSystemMode;
        this.clockUrl = clockUrl;
        this.observationRegistry = observationRegistry;
        this.http = new GatewayHttpClient("clock", resilience);
    }

    public Instant getCurrentTime() {
        if ("real".equals(externalSystemMode)) {
            return Instant.now();
        } else if ("stub".equals(externalSystemMode)) {
            return getStubTime();
        } else {
            throw new IllegalStateException("Unknown external system mode: " + externalSystemMode);
        }
    }

    private Instant getStubTime() {
        try {
            var url = clockUrl + "/api/time";
            var response = http.get(url, observationRegistry);

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Clock API returned status " + response.statusCode() +
                        ". URL: " + url + ". Response: " + response.body());
            }

            var clockResponse = OBJECT_MAPPER.readValue(response.body(), ClockGetTimeResponse.class);
            return clockResponse.getTime();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch current time from URL: " + clockUrl +
                    ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch current time from URL: " + clockUrl +
                    ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
