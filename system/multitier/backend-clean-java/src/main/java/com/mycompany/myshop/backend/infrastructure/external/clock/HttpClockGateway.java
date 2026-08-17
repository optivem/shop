package com.mycompany.myshop.backend.infrastructure.external.clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * Supplies the current time: the real system clock in {@code real} mode, or an HTTP call to the
 * controllable clock stub in {@code stub} mode. Which of the two is an infrastructure concern — the
 * domain sees only an {@link Instant}.
 */
@Service
public class HttpClockGateway implements ClockGateway {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final String externalSystemMode;
    private final String clockUrl;

    public HttpClockGateway(
            @Value("${external.system-mode}") String externalSystemMode,
            @Value("${clock.url}") String clockUrl) {
        this.externalSystemMode = externalSystemMode;
        this.clockUrl = clockUrl;
    }

    @Override
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
            var httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            var url = clockUrl + "/api/time";
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Clock API returned status " + response.statusCode() +
                        ". URL: " + url + ". Response: " + response.body());
            }

            var clockResponse = OBJECT_MAPPER.readValue(response.body(), GetTimeResponse.class);
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
