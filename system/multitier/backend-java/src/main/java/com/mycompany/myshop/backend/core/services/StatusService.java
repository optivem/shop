package com.mycompany.myshop.backend.core.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Answers "is this part of the system up?" for the storefront's wake-up screen. The external probes are
// plain GETs with no retry and deliberately bypass the gateways (and their circuit breakers): a probe
// must not trip the breaker that real traffic uses. Any failure is DOWN, never an exception.
@Service
public class StatusService {

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(10);
    private static final int DB_VALID_TIMEOUT_SECONDS = 5;

    private final DataSource dataSource;
    private final String externalSystemMode;
    private final String erpUrl;
    private final String taxUrl;
    private final String clockUrl;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(PROBE_TIMEOUT).build();

    public StatusService(
            DataSource dataSource,
            @Value("${external.system-mode}") String externalSystemMode,
            @Value("${erp.url}") String erpUrl,
            @Value("${tax.url}") String taxUrl,
            @Value("${clock.url}") String clockUrl) {
        this.dataSource = dataSource;
        this.externalSystemMode = externalSystemMode;
        this.erpUrl = erpUrl;
        this.taxUrl = taxUrl;
        this.clockUrl = clockUrl;
    }

    /** Empty for an unknown system; otherwise whether it is up. */
    public Optional<Boolean> isUp(String system) {
        return switch (system) {
            case "backend" -> Optional.of(databaseAnswers());
            case "erp" -> Optional.of(healthEndpointAnswers(erpUrl));
            case "tax" -> Optional.of(healthEndpointAnswers(taxUrl));
            case "clock" -> Optional.of("real".equals(externalSystemMode) || healthEndpointAnswers(clockUrl));
            default -> Optional.empty();
        };
    }

    private boolean databaseAnswers() {
        try (var connection = dataSource.getConnection()) {
            return connection.isValid(DB_VALID_TIMEOUT_SECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean healthEndpointAnswers(String baseUrl) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(PROBE_TIMEOUT)
                    .GET()
                    .build();
            var status = httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            return status >= 200 && status < 300;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
