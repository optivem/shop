package com.mycompany.myshop.backend.core.services.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycompany.myshop.backend.core.dtos.external.ClockGetTimeResponse;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.java11.instrument.binder.jdk.MicrometerHttpClient;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

@Service
public class ClockGateway {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final String externalSystemMode;
    private final String clockUrl;
    private final ObservationRegistry observationRegistry;

    public ClockGateway(String externalSystemMode, String clockUrl) {
        this(externalSystemMode, clockUrl, ObservationRegistry.NOOP);
    }

    // The registry gives each outbound call a client span and a W3C traceparent header.
    @Autowired
    public ClockGateway(
            @Value("${external.system-mode}") String externalSystemMode,
            @Value("${clock.url}") String clockUrl,
            ObservationRegistry observationRegistry) {
        this.externalSystemMode = externalSystemMode;
        this.clockUrl = clockUrl;
        this.observationRegistry = observationRegistry;
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
            var httpClient = MicrometerHttpClient.instrumentationBuilder(HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build(), Metrics.globalRegistry)
                    .observationRegistry(observationRegistry)
                    .build();

            var url = clockUrl + "/api/time";
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

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
