package com.mycompany.myshop.testkit.driver.adapter.external.identity.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Keycloak's admin REST API, for the test realm of a local Keycloak only (the docker-compose stacks). It signs in
 * to the master realm as the bootstrap admin, which the stacks set to admin/admin (KC_BOOTSTRAP_ADMIN_*);
 * KEYCLOAK_ADMIN_USERNAME / KEYCLOAK_ADMIN_PASSWORD override that. A Keycloak that is not on this machine is
 * refused before any call, so the shared cloud Keycloak and its QA/production realms are never touched.
 */
public class KeycloakAdminClient {
    public static final String REALM = "shop";
    private static final String ADMIN_CLIENT_ID = "admin-cli";
    private static final String DEFAULT_ADMIN = "admin";
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");

    private final String baseUrl;
    private final String adminUsername;
    private final String adminPassword;
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KeycloakAdminClient(String keycloakBaseUrl) {
        this(keycloakBaseUrl, envOrDefault("KEYCLOAK_ADMIN_USERNAME"), envOrDefault("KEYCLOAK_ADMIN_PASSWORD"));
    }

    public KeycloakAdminClient(String keycloakBaseUrl, String adminUsername, String adminPassword) {
        this.baseUrl = requireLocal(keycloakBaseUrl);
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    /** Creates an enabled user with a permanent password and returns its id. */
    public String createUser(String username, String password, String firstName, String lastName, String email) {
        var user = Map.of(
                "username", username,
                "enabled", true,
                "firstName", firstName,
                "lastName", lastName,
                "email", email,
                "emailVerified", true,
                "requiredActions", List.of(),
                "credentials", List.of(Map.of("type", "password", "value", password, "temporary", false)));
        var response = send(adminRequest("/users")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(user))));
        expectStatus(response, 201, "create user " + username);
        var location = response.headers().firstValue("Location")
                .orElseThrow(() -> new IllegalStateException("Keycloak created user " + username
                        + " in realm " + REALM + " but sent no Location header"));
        return location.substring(location.lastIndexOf('/') + 1);
    }

    /** Gives the user a realm role, e.g. CUSTOMER. */
    public void assignRealmRole(String userId, String roleName) {
        var roleResponse = send(adminRequest("/roles/" + encode(roleName)).GET());
        expectStatus(roleResponse, 200, "read realm role " + roleName);
        JsonNode role = parse(roleResponse.body());
        var mapping = send(adminRequest("/users/" + encode(userId) + "/role-mappings/realm")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("[" + role + "]")));
        expectStatus(mapping, 204, "give user " + userId + " the realm role " + roleName);
    }

    /** Deletes the user; one that is already gone counts as deleted. */
    public void deleteUser(String userId) {
        var response = send(adminRequest("/users/" + encode(userId)).DELETE());
        if (response.statusCode() != 404) {
            expectStatus(response, 204, "delete user " + userId);
        }
    }

    private HttpRequest.Builder adminRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/realms/" + REALM + path))
                .header("Authorization", "Bearer " + adminToken());
    }

    /** A fresh master-realm admin token (they live for a minute, so one per call keeps it simple). */
    private String adminToken() {
        var form = "grant_type=password&client_id=" + encode(ADMIN_CLIENT_ID)
                + "&username=" + encode(adminUsername) + "&password=" + encode(adminPassword);
        var response = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/realms/master/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)));
        if (response.statusCode() == 401) {
            throw new IllegalStateException("Keycloak at " + baseUrl + " refused the admin sign-in as '" + adminUsername
                    + "' (HTTP 401). The local stacks use admin/admin (KC_BOOTSTRAP_ADMIN_*); set"
                    + " KEYCLOAK_ADMIN_USERNAME and KEYCLOAK_ADMIN_PASSWORD if yours differ.");
        }
        expectStatus(response, 200, "get an admin token");
        return parse(response.body()).path("access_token").asText();
    }

    private HttpResponse<String> send(HttpRequest.Builder request) {
        var built = request.build();
        try {
            return httpClient.send(built, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException("Keycloak admin call " + built.method() + " " + built.uri() + " failed: " + e, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during Keycloak admin call " + built.uri(), e);
        }
    }

    private void expectStatus(HttpResponse<String> response, int expected, String action) {
        if (response.statusCode() != expected) {
            throw new IllegalStateException("Keycloak at " + baseUrl + " could not " + action + " in realm " + REALM
                    + ": HTTP " + response.statusCode() + " " + response.body());
        }
    }

    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (IOException e) {
            throw new IllegalStateException("Keycloak at " + baseUrl + " sent malformed JSON: " + body, e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write Keycloak admin request", e);
        }
    }

    private static String requireLocal(String keycloakBaseUrl) {
        if (keycloakBaseUrl == null || keycloakBaseUrl.isBlank()) {
            throw new IllegalStateException("A new customer needs Keycloak: set KEYCLOAK_URL (or KEYCLOAK_URL_<MODE>)"
                    + " to the local stack's Keycloak, e.g. http://localhost:8192.");
        }
        var host = URI.create(keycloakBaseUrl).getHost();
        if (host == null || !LOCAL_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("A new customer is created only in a local Keycloak (localhost), never in a"
                    + " shared one; KEYCLOAK_URL is " + keycloakBaseUrl + ". Run this scenario against a local stack.");
        }
        return keycloakBaseUrl.endsWith("/") ? keycloakBaseUrl.substring(0, keycloakBaseUrl.length() - 1) : keycloakBaseUrl;
    }

    private static String envOrDefault(String name) {
        var value = System.getenv(name);
        return value == null || value.isBlank() ? DEFAULT_ADMIN : value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
