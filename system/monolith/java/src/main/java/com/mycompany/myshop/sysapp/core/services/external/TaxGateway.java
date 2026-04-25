package com.mycompany.myshop.sysapp.core.services.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.sysapp.core.dtos.external.TaxDetailsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

@Service
public class TaxGateway {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${tax.url}")
    private String taxUrl;

    public Optional<TaxDetailsResponse> getTaxDetails(String country) {
        var url = taxUrl + "/api/countries/" + country;
        System.out.println("getTaxDetails - url: " + url);

        try {
            var httpClient = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("getTaxDetails - status code was: " + response.statusCode());

            if (response.statusCode() == 404) {
                System.out.println("getTaxDetails - status code was 404");
                return Optional.empty();  // Country not found
            }

            if (response.statusCode() != 200) {
                throw new RuntimeException("Tax API returned status " + response.statusCode() +
                        " for country: " + country + ". URL: " + url + ". Response: " + response.body());
            }

            var result = OBJECT_MAPPER.readValue(response.body(), TaxDetailsResponse.class);
            return Optional.of(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch tax details for country: " + country +
                    " from URL: " + url + ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
