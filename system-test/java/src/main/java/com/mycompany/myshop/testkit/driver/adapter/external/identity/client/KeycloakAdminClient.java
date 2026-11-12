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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Keycloak's admin REST API, for the test realm only. How it signs in depends on where Keycloak is:
 * <ul>
 * <li>A local Keycloak (localhost, the docker-compose stacks): the master realm's bootstrap admin, which the stacks
 * set to admin/admin (KC_BOOTSTRAP_ADMIN_*); KEYCLOAK_ADMIN_USERNAME / KEYCLOAK_ADMIN_PASSWORD override that.</li>
 * <li>Any other Keycloak (the shared cloud one): a confidential service-account client of the test realm itself,
 * holding only realm-management's user roles (client-credentials grant, KEYCLOAK_TEST_ADMIN_CLIENT_ID /
 * KEYCLOAK_TEST_ADMIN_CLIENT_SECRET). KEYCLOAK_TEST_ADMIN_REALM must name the test realm, and the production realm
 * is refused whatever the config says. Both checks run before any call, and the master admin is never used.</li>
 * </ul>
 */
public class KeycloakAdminClient {
    public static final String REALM = "shop";
    private static final String PRODUCTION_REALM = "shop-prod";
    private static final String ADMIN_CLIENT_ID = "admin-cli";
    private static final String DEFAULT_ADMIN = "admin";
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");

    private final String baseUrl;
    private final TokenGrant tokenGrant;
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** How to get an admin token: the token endpoint, its form, and what to say when Keycloak refuses it. */
    private record TokenGrant(String tokenUrl, String form, String refusedMessage) {
    }

    public KeycloakAdminClient(String keycloakBaseUrl) {
        this.baseUrl = requireUrl(keycloakBaseUrl);
        this.tokenGrant = isLocal(baseUrl)
                ? masterAdminGrant(baseUrl, envOrDefault("KEYCLOAK_ADMIN_USERNAME"), envOrDefault("KEYCLOAK_ADMIN_PASSWORD"))
                : serviceAccountGrant(baseUrl);
    }

    /** Signs in as the given master-realm admin; a local Keycloak only. */
    public KeycloakAdminClient(String keycloakBaseUrl, String adminUsername, String adminPassword) {
        this.baseUrl = requireUrl(keycloakBaseUrl);
        if (!isLocal(baseUrl)) {
            throw new IllegalStateException("The master-realm admin is used only for a local Keycloak (localhost), never"
                    + " for a shared one; KEYCLOAK_URL is " + baseUrl + ".");
        }
        this.tokenGrant = masterAdminGrant(baseUrl, adminUsername, adminPassword);
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

    /**
     * Gives the user a realm role, e.g. CUSTOMER. The role is looked up among the roles the user can be given, which
     * needs only view-users (reading the realm's roles directly needs view-realm, which the service account lacks).
     */
    public void assignRealmRole(String userId, String roleName) {
        var available = send(adminRequest("/users/" + encode(userId) + "/role-mappings/realm/available").GET());
        expectStatus(available, 200, "list the realm roles user " + userId + " can be given");
        JsonNode role = null;
        for (var candidate : parse(available.body())) {
            if (roleName.equals(candidate.path("name").asText())) {
                role = candidate;
            }
        }
        if (role == null) {
            throw new IllegalStateException("Keycloak at " + baseUrl + " has no realm role " + roleName
                    + " that user " + userId + " can be given in realm " + REALM + ": " + available.body());
        }
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

    /** A fresh admin token (they live for a minute, so one per call keeps it simple). */
    private String adminToken() {
        var response = send(HttpRequest.newBuilder()
                .uri(URI.create(tokenGrant.tokenUrl()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(tokenGrant.form())));
        if (response.statusCode() == 401) {
            throw new IllegalStateException(tokenGrant.refusedMessage());
        }
        expectStatus(response, 200, "get an admin token");
        var token = parse(response.body()).path("access_token").asText();
        if (token.isEmpty()) {
            throw new IllegalStateException("Keycloak at " + baseUrl + " sent an admin token response without"
                    + " access_token");
        }
        return token;
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
        if (response.statusCode() == 403) {
            throw new IllegalStateException("Keycloak at " + baseUrl + " did not allow the admin account to " + action
                    + " in realm " + REALM + " (HTTP 403). The service account needs realm-management's manage-users,"
                    + " view-users and query-users, which the acceptance stage's deploy-keycloak job grants: "
                    + response.body());
        }
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

    private static TokenGrant masterAdminGrant(String baseUrl, String adminUsername, String adminPassword) {
        return new TokenGrant(baseUrl + "/realms/master/protocol/openid-connect/token",
                "grant_type=password&client_id=" + encode(ADMIN_CLIENT_ID)
                        + "&username=" + encode(adminUsername) + "&password=" + encode(adminPassword),
                "Keycloak at " + baseUrl + " refused the admin sign-in as '" + adminUsername + "' (HTTP 401). The local"
                        + " stacks use admin/admin (KC_BOOTSTRAP_ADMIN_*); set KEYCLOAK_ADMIN_USERNAME and"
                        + " KEYCLOAK_ADMIN_PASSWORD if yours differ.");
    }

    /**
     * The test realm's service account, for a shared Keycloak. Refuses the production realm, and any realm that
     * KEYCLOAK_TEST_ADMIN_REALM does not name, before anything is sent.
     */
    private static TokenGrant serviceAccountGrant(String baseUrl) {
        var clientId = System.getenv("KEYCLOAK_TEST_ADMIN_CLIENT_ID");
        var clientSecret = System.getenv("KEYCLOAK_TEST_ADMIN_CLIENT_SECRET");
        var allowedRealm = System.getenv("KEYCLOAK_TEST_ADMIN_REALM");
        if (PRODUCTION_REALM.equals(allowedRealm)) {
            throw new IllegalStateException("Refusing to create users in the production realm " + PRODUCTION_REALM
                    + " of the Keycloak at " + baseUrl + ": KEYCLOAK_TEST_ADMIN_REALM must name the test realm ("
                    + REALM + "). Nothing was sent to Keycloak.");
        }
        var missing = new ArrayList<String>();
        if (isBlank(clientId)) {
            missing.add("KEYCLOAK_TEST_ADMIN_CLIENT_ID");
        }
        if (isBlank(clientSecret)) {
            missing.add("KEYCLOAK_TEST_ADMIN_CLIENT_SECRET");
        }
        if (isBlank(allowedRealm)) {
            missing.add("KEYCLOAK_TEST_ADMIN_REALM");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("A new customer in the shared Keycloak at " + baseUrl + " needs the test"
                    + " realm's service account, but " + String.join(", ", missing) + " is not set. Set"
                    + " KEYCLOAK_TEST_ADMIN_CLIENT_ID (shop-system-test-admin), KEYCLOAK_TEST_ADMIN_CLIENT_SECRET and"
                    + " KEYCLOAK_TEST_ADMIN_REALM=" + REALM + " (the cloud acceptance stage sets them, the secret from"
                    + " the KEYCLOAK_TEST_ADMIN_CLIENT_SECRET GitHub secret), or run against a local stack.");
        }
        if (!REALM.equals(allowedRealm)) {
            throw new IllegalStateException("KEYCLOAK_TEST_ADMIN_REALM is '" + allowedRealm + "', but new customers"
                    + " are created in realm " + REALM + ", which it does not allow. Set KEYCLOAK_TEST_ADMIN_REALM="
                    + REALM + ". Nothing was sent to the Keycloak at " + baseUrl + ".");
        }
        return new TokenGrant(baseUrl + "/realms/" + REALM + "/protocol/openid-connect/token",
                "grant_type=client_credentials&client_id=" + encode(clientId) + "&client_secret=" + encode(clientSecret),
                "Keycloak at " + baseUrl + " refused the service account '" + clientId + "' of realm " + REALM
                        + " (HTTP 401). KEYCLOAK_TEST_ADMIN_CLIENT_SECRET must match the client's secret, which the"
                        + " acceptance stage's deploy-keycloak job sets from the GitHub secret of the same name; check"
                        + " that secret and re-run the stage.");
    }

    private static String requireUrl(String keycloakBaseUrl) {
        if (isBlank(keycloakBaseUrl)) {
            throw new IllegalStateException("A new customer needs Keycloak: set KEYCLOAK_URL (or KEYCLOAK_URL_<MODE>)"
                    + " to the stack's Keycloak, e.g. http://localhost:8192.");
        }
        if (URI.create(keycloakBaseUrl).getHost() == null) {
            throw new IllegalStateException("KEYCLOAK_URL is not an absolute URL: " + keycloakBaseUrl);
        }
        return keycloakBaseUrl.endsWith("/") ? keycloakBaseUrl.substring(0, keycloakBaseUrl.length() - 1) : keycloakBaseUrl;
    }

    private static boolean isLocal(String keycloakBaseUrl) {
        return LOCAL_HOSTS.contains(URI.create(keycloakBaseUrl).getHost().toLowerCase(Locale.ROOT));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String envOrDefault(String name) {
        var value = System.getenv(name);
        return isBlank(value) ? DEFAULT_ADMIN : value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
