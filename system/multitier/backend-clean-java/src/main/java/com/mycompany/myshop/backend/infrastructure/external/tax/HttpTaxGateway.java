package com.mycompany.myshop.backend.infrastructure.external.tax;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.domain.values.TaxRate;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.gateways.TaxGatewayException;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.infrastructure.external.GatewayHttpClient;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class HttpTaxGateway implements TaxGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpTaxGateway.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String taxUrl;
    private final ObservationRegistry observationRegistry;

    public HttpTaxGateway(String taxUrl) {
        this(taxUrl, ObservationRegistry.NOOP);
    }

    @Autowired
    public HttpTaxGateway(@Value("${tax.url}") String taxUrl, ObservationRegistry observationRegistry) {
        this.taxUrl = taxUrl;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public Optional<TaxRate> getTaxDetails(Country country) {
        return fetchTaxDetails(country.value())
                .map(wire -> new TaxRate(Country.of(wire.getCountryName()), Rate.of(wire.getTaxRate())));
    }

    // Private for the same reason as HttpErpGateway's and HttpClockGateway's: TaxDetailsResponse is
    // the tax service's shape, so getTaxDetails above is the only place allowed to hold one.
    private Optional<TaxDetailsResponse> fetchTaxDetails(String country) {
        var url = taxUrl + "/api/countries/" + country;
        if (log.isInfoEnabled()) {
            log.info("getTaxDetails - url: {}", url.replaceAll("[\r\n]", "_"));
        }

        try {
            var response = GatewayHttpClient.get(url, observationRegistry);

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
