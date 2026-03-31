package com.optivem.starter.backend.core.services.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.optivem.starter.backend.core.dtos.external.GetTimeResponse;
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

    @Value("${external.system-mode}")
    private String externalSystemMode;

    @Value("${clock.url}")
    private String clockUrl;

    public Instant getCurrentTime() {
        if ("real".equals(externalSystemMode)) {
            return Instant.now();
        } else if ("stub".equals(externalSystemMode)) {
            return getStubTime();
        } else {
            throw new RuntimeException("Unknown external system mode: " + externalSystemMode);
        }
    }

    private Instant getStubTime() {
        try {
            var httpClient = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();

            var url = clockUrl + "/api/time";
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Clock API returned status " + response.statusCode() +
                        ". URL: " + url + ". Response: " + response.body());
            }

            var clockResponse = OBJECT_MAPPER.readValue(response.body(), GetTimeResponse.class);
            return clockResponse.getTime();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch current time from URL: " + clockUrl +
                    ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
