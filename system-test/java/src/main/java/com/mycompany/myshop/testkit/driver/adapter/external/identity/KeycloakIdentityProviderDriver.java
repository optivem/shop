package com.mycompany.myshop.testkit.driver.adapter.external.identity;

import com.mycompany.myshop.testkit.common.Result;
import com.mycompany.myshop.testkit.driver.adapter.external.identity.client.KeycloakAdminClient;
import com.mycompany.myshop.testkit.driver.adapter.external.identity.client.KeycloakRealmClient;
import com.mycompany.myshop.testkit.driver.port.UserAccount;
import com.mycompany.myshop.testkit.driver.port.external.identity.IdentityProviderDriver;
import com.mycompany.myshop.testkit.driver.port.external.identity.dtos.error.IdentityProviderErrorResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Creates customers and admins in Keycloak's test realm, each with a unique username and a random password, and
 * deletes them again on close. The admin client signs in only when a user is first created, so
 * {@link #goToIdentityProvider()} needs no admin credentials.
 */
public class KeycloakIdentityProviderDriver implements IdentityProviderDriver {
    private static final String CUSTOMER_ROLE = "CUSTOMER";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String CUSTOMER_USERNAME_PREFIX = "new-customer-";
    private static final String ADMIN_USERNAME_PREFIX = "new-admin-";

    private final String keycloakBaseUrl;
    private final KeycloakRealmClient realmClient;
    private final List<String> createdUserIds = new ArrayList<>();
    private KeycloakAdminClient adminClient;

    public KeycloakIdentityProviderDriver(String keycloakBaseUrl) {
        this.keycloakBaseUrl = keycloakBaseUrl;
        this.realmClient = new KeycloakRealmClient(keycloakBaseUrl);
    }

    @Override
    public Result<Void, IdentityProviderErrorResponse> goToIdentityProvider() {
        return realmClient.checkHealth().mapError(IdentityProviderErrorResponse::new);
    }

    @Override
    public UserAccount createCustomer(String firstName, String lastName, String email) {
        return createUser(CUSTOMER_USERNAME_PREFIX, CUSTOMER_ROLE, firstName, lastName, email);
    }

    @Override
    public UserAccount createAdmin(String firstName, String lastName, String email) {
        return createUser(ADMIN_USERNAME_PREFIX, ADMIN_ROLE, firstName, lastName, email);
    }

    @Override
    public UserAccount newAccountCredentials() {
        return new UserAccount(CUSTOMER_USERNAME_PREFIX + UUID.randomUUID(), UUID.randomUUID().toString());
    }

    @Override
    public String newAccountEmail(UserAccount account) {
        return account.username() + "@example.com";
    }

    @Override
    public void deleteOnClose(String username) {
        createdUserIds.add(client().findUserId(username));
    }

    private UserAccount createUser(String usernamePrefix, String role, String firstName, String lastName, String email) {
        var username = usernamePrefix + UUID.randomUUID();
        var password = UUID.randomUUID().toString();
        var userId = client().createUser(username, password, firstName, lastName,
                email != null ? email : username + "@example.com");
        createdUserIds.add(userId);
        client().assignRealmRole(userId, role);
        if (ADMIN_ROLE.equals(role)) {
            // Every new user gets the realm's default role, which makes a visitor who signs up a customer (UC-013).
            // An admin is not a customer.
            client().removeDefaultRoles(userId);
        }
        return new UserAccount(username, password);
    }

    private KeycloakAdminClient client() {
        if (adminClient == null) {
            adminClient = new KeycloakAdminClient(keycloakBaseUrl);
        }
        return adminClient;
    }

    @Override
    public void close() {
        var failures = new ArrayList<String>();
        for (var userId : createdUserIds) {
            try {
                client().deleteUser(userId);
            } catch (IllegalStateException e) {
                failures.add(e.getMessage());
            }
        }
        createdUserIds.clear();
        if (!failures.isEmpty()) {
            throw new IllegalStateException("Could not delete the users created for this scenario: " + failures);
        }
    }
}
