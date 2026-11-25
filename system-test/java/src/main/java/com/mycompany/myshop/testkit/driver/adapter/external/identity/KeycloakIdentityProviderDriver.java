package com.mycompany.myshop.testkit.driver.adapter.external.identity;

import com.mycompany.myshop.testkit.driver.adapter.external.identity.client.KeycloakAdminClient;
import com.mycompany.myshop.testkit.driver.port.UserAccount;
import com.mycompany.myshop.testkit.driver.port.external.identity.IdentityProviderDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Creates customers and admins in Keycloak's test realm, each with a unique username and a random password, and
 * deletes them again on close.
 */
public class KeycloakIdentityProviderDriver implements IdentityProviderDriver {
    private static final String CUSTOMER_ROLE = "CUSTOMER";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String CUSTOMER_USERNAME_PREFIX = "new-customer-";
    private static final String ADMIN_USERNAME_PREFIX = "new-admin-";

    private final KeycloakAdminClient client;
    private final List<String> createdUserIds = new ArrayList<>();

    public KeycloakIdentityProviderDriver(String keycloakBaseUrl) {
        this.client = new KeycloakAdminClient(keycloakBaseUrl);
    }

    @Override
    public UserAccount createCustomer(String firstName, String lastName, String email) {
        return createUser(CUSTOMER_USERNAME_PREFIX, CUSTOMER_ROLE, firstName, lastName, email);
    }

    @Override
    public UserAccount createAdmin(String firstName, String lastName, String email) {
        return createUser(ADMIN_USERNAME_PREFIX, ADMIN_ROLE, firstName, lastName, email);
    }

    private UserAccount createUser(String usernamePrefix, String role, String firstName, String lastName, String email) {
        var username = usernamePrefix + UUID.randomUUID();
        var password = UUID.randomUUID().toString();
        var userId = client.createUser(username, password, firstName, lastName,
                email != null ? email : username + "@example.com");
        createdUserIds.add(userId);
        client.assignRealmRole(userId, role);
        return new UserAccount(username, password);
    }

    @Override
    public void close() {
        var failures = new ArrayList<String>();
        for (var userId : createdUserIds) {
            try {
                client.deleteUser(userId);
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
