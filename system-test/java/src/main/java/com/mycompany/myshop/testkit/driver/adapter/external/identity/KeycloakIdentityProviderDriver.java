package com.mycompany.myshop.testkit.driver.adapter.external.identity;

import com.mycompany.myshop.testkit.driver.adapter.external.identity.client.KeycloakAdminClient;
import com.mycompany.myshop.testkit.driver.port.CustomerAccount;
import com.mycompany.myshop.testkit.driver.port.external.identity.IdentityProviderDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Creates customers in the local Keycloak's test realm, each with a unique username and a random password, and
 * deletes them again on close.
 */
public class KeycloakIdentityProviderDriver implements IdentityProviderDriver {
    private static final String CUSTOMER_ROLE = "CUSTOMER";
    private static final String USERNAME_PREFIX = "new-customer-";

    private final KeycloakAdminClient client;
    private final List<String> createdUserIds = new ArrayList<>();

    public KeycloakIdentityProviderDriver(String keycloakBaseUrl) {
        this.client = new KeycloakAdminClient(keycloakBaseUrl);
    }

    @Override
    public CustomerAccount createCustomer(String firstName, String lastName, String email) {
        var username = USERNAME_PREFIX + UUID.randomUUID();
        var password = UUID.randomUUID().toString();
        var userId = client.createUser(username, password, firstName, lastName,
                email != null ? email : username + "@example.com");
        createdUserIds.add(userId);
        client.assignRealmRole(userId, CUSTOMER_ROLE);
        return new CustomerAccount(username, password);
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
            throw new IllegalStateException("Could not delete the customers created for this scenario: " + failures);
        }
    }
}
