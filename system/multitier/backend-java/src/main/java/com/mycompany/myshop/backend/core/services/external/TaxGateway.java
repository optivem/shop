package com.mycompany.myshop.backend.core.services.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.core.dtos.external.TaxDetailsResponse;
import com.mycompany.myshop.backend.core.exceptions.TaxGatewayException;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class TaxGateway {

    private static final Logger log = LoggerFactory.getLogger(TaxGateway.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String taxUrl;
    private final ObservationRegistry observationRegistry;
    private final GatewayHttpClient http;

    public TaxGateway(String taxUrl) {
        this(taxUrl, ResilienceSettings.DEFAULTS);
    }

    public TaxGateway(String taxUrl, ResilienceSettings resilience) {
        this(taxUrl, ObservationRegistry.NOOP, resilience);
    }

    // The registry gives each outbound call a client span and a W3C traceparent header.
    @Autowired
    public TaxGateway(@Value("${tax.url}") String taxUrl, ObservationRegistry observationRegistry,
            ResilienceSettings resilience) {
        this.taxUrl = taxUrl;
        this.observationRegistry = observationRegistry;
        this.http = new GatewayHttpClient("tax", resilience);
    }

    public Optional<TaxDetailsResponse> getTaxDetails(String country) {
        var url = taxUrl + "/api/countries/" + country;
        if (log.isInfoEnabled()) {
            log.info("getTaxDetails - url: {}", url.replaceAll("[\r\n]", "_"));
        }

        try {
            var response = http.get(url, observationRegistry);

            log.info("getTaxDetails - status code was: {}", response.statusCode());

            if (response.statusCode() == 404) {
                log.info("getTaxDetails - status code was 404");
                return Optional.empty();  // Country not found
            }

            if (response.statusCode() != 200) {
                throw new TaxGatewayException("Tax API returned status " + response.statusCode() +
                        " for country: " + country + ". URL: " + url + ". Response: " + response.body());
            }

            var result = OBJECT_MAPPER.readValue(response.body(), TaxDetailsResponse.class);
            return Optional.of(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TaxGatewayException("Interrupted while fetching tax details for country: " + country +
                    " from URL: " + url, e);
        } catch (IOException e) {
            throw new TaxGatewayException("Failed to fetch tax details for country: " + country +
                    " from URL: " + url + ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
