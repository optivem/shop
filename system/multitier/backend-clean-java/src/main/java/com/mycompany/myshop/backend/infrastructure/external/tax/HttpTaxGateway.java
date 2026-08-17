package com.mycompany.myshop.backend.infrastructure.external.tax;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.domain.entities.TaxRate;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.exceptions.TaxGatewayException;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Talks HTTP to the tax system and maps its wire shape to {@link TaxRate}. A 404 becomes an empty
 * {@code Optional}; the domain never sees a status code.
 */
@Service
public class HttpTaxGateway implements TaxGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpTaxGateway.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String taxUrl;

    public HttpTaxGateway(@Value("${tax.url}") String taxUrl) {
        this.taxUrl = taxUrl;
    }

    @Override
    public Optional<TaxRate> getTaxDetails(String country) {
        return fetchTaxDetails(country)
                .map(wire -> new TaxRate(wire.getId(), wire.getCountryName(), Rate.of(wire.getTaxRate())));
    }

    /**
     * The raw tax-system call and parse, before the mapping to {@link TaxRate}. Exposed on the
     * adapter (rather than on the {@link TaxGateway} port) so the stub-contract component tests can
     * read the stub's bytes back through the SUT's real HTTP call and real Jackson parse.
     */
    public Optional<TaxDetailsResponse> fetchTaxDetails(String country) {
        var url = taxUrl + "/api/countries/" + country;
        if (log.isInfoEnabled()) {
            log.info("getTaxDetails - url: {}", url.replaceAll("[\r\n]", "_"));
        }

        try {
            var httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

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
