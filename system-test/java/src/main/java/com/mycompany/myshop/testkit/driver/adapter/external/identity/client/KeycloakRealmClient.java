package com.mycompany.myshop.testkit.driver.adapter.external.identity.client;

import com.mycompany.myshop.testkit.common.Result;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * The test realm's public endpoints, which need no sign-in. Separate from {@link KeycloakAdminClient}, so checking
 * that Keycloak is up never needs admin credentials.
 */
public class KeycloakRealmClient {
    private static final int MAX_BODY_IN_MESSAGE = 300;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String baseUrl;

    public KeycloakRealmClient(String keycloakBaseUrl) {
        this.baseUrl = keycloakBaseUrl.endsWith("/")
                ? keycloakBaseUrl.substring(0, keycloakBaseUrl.length() - 1)
                : keycloakBaseUrl;
    }

    /**
     * The realm's OpenID discovery document answers 200 once Keycloak has finished bootstrapping and serves the
     * realm; during the bootstrap it answers 503.
     */
    public Result<Void, String> checkHealth() {
        var url = baseUrl + "/realms/" + KeycloakAdminClient.REALM + "/.well-known/openid-configuration";
        var request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
        try {
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return Result.success();
            }
            var hint = response.statusCode() == 503 ? " (503: Keycloak is still bootstrapping)" : "";
            return Result.failure("Keycloak realm " + KeycloakAdminClient.REALM + " at " + url + " answered HTTP "
                    + response.statusCode() + hint + ": " + truncate(response.body()));
        } catch (IOException e) {
            return Result.failure("Keycloak at " + url + " could not be reached: " + e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure("Interrupted while checking Keycloak at " + url);
        }
    }

    private static String truncate(String body) {
        return body.length() <= MAX_BODY_IN_MESSAGE ? body : body.substring(0, MAX_BODY_IN_MESSAGE) + "...";
    }
}
