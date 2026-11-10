package com.mycompany.myshop.testkit.driver.adapter.shared.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Acquires access tokens with the password grant (test realm only). Tokens are cached per
 * Keycloak instance and user for the lifetime of the JVM and refreshed shortly before expiry.
 * A transport failure or a 5xx is retried a few times, since Keycloak can drop connections under
 * parallel test load; a 4xx (e.g. bad credentials) is a definitive answer and fails at once.
 */
public class KeycloakTokenProvider {
    private static final String CLIENT_ID = "shop-system-test";
    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(30);
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(500);
    private static final Map<String, KeycloakTokenProvider> PROVIDERS = new ConcurrentHashMap<>();

    private record CachedToken(String value, Instant expiresAt) {
    }

    private final URI tokenUri;
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<TestUser, CachedToken> cache = new ConcurrentHashMap<>();

    private KeycloakTokenProvider(String keycloakBaseUrl) {
        this.tokenUri = URI.create(keycloakBaseUrl + "/realms/shop/protocol/openid-connect/token");
    }

    public static KeycloakTokenProvider forBaseUrl(String keycloakBaseUrl) {
        return PROVIDERS.computeIfAbsent(keycloakBaseUrl, KeycloakTokenProvider::new);
    }

    public String getToken(TestUser user) {
        var cached = cache.get(user);
        if (isValid(cached)) {
            return cached.value();
        }
        synchronized (this) {
            cached = cache.get(user);
            if (isValid(cached)) {
                return cached.value();
            }
            var fresh = fetchToken(user);
            cache.put(user, fresh);
            return fresh.value();
        }
    }

    private static boolean isValid(CachedToken token) {
        return token != null && Instant.now().isBefore(token.expiresAt());
    }

    private CachedToken fetchToken(TestUser user) {
        for (var attempt = 1; ; attempt++) {
            try {
                return fetchTokenOnce(user);
            } catch (TransientTokenException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw e;
                }
                System.err.println("[Keycloak] attempt " + attempt + "/" + MAX_ATTEMPTS + " failed, retrying: " + e.getMessage());
                sleep(RETRY_DELAY.multipliedBy(attempt));
            }
        }
    }

    private CachedToken fetchTokenOnce(TestUser user) {
        var form = "grant_type=password"
                + "&client_id=" + encode(CLIENT_ID)
                + "&username=" + encode(user.getUsername())
                + "&password=" + encode(user.getPassword());
        var request = HttpRequest.newBuilder()
                .uri(tokenUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 500) {
                throw new TransientTokenException("Failed to acquire token for " + user.getUsername() + " from "
                        + tokenUri + ": HTTP " + response.statusCode() + " " + response.body(), null);
            }
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Failed to acquire token for " + user.getUsername() + " from "
                        + tokenUri + ": HTTP " + response.statusCode() + " " + response.body());
            }
            var json = objectMapper.readTree(response.body());
            var expiresIn = json.path("expires_in").asLong(60);
            var expiresAt = Instant.now().plusSeconds(expiresIn).minus(EXPIRY_MARGIN);
            return new CachedToken(json.get("access_token").asText(), expiresAt);
        } catch (IOException e) {
            throw new TransientTokenException("Failed to acquire token for " + user.getUsername() + " from " + tokenUri
                    + ": " + e, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while acquiring token from " + tokenUri, e);
        }
    }

    private static void sleep(Duration delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry token acquisition", e);
        }
    }

    /** A failure worth retrying; after the last attempt it surfaces as is. */
    private static final class TransientTokenException extends IllegalStateException {
        TransientTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
