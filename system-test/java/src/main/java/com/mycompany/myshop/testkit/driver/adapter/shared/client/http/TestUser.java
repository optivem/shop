package com.mycompany.myshop.testkit.driver.adapter.shared.client.http;

import com.mycompany.myshop.testkit.driver.port.UserIdentity;

import java.util.Objects;

/**
 * Credentials the drivers sign in with: the test realm's shared customers, or a customer or admin account created
 * for the scenario. The shared customers are singletons, so they can be compared with {@code ==}.
 */
public final class TestUser {
    public static final TestUser CUSTOMER = new TestUser("customer1", "customer1-test-password", false);
    public static final TestUser CUSTOMER2 = new TestUser("customer2", "customer2-test-password", false);

    private final String username;
    private final String password;
    private final boolean admin;

    private TestUser(String username, String password, boolean admin) {
        this.username = username;
        this.password = password;
        this.admin = admin;
    }

    /** The shared test customer at the given zero-based position (0 = customer1). */
    public static TestUser customer(int index) {
        return switch (index) {
            case 0 -> CUSTOMER;
            case 1 -> CUSTOMER2;
            default -> throw new IllegalArgumentException("No test customer at index " + index);
        };
    }

    /** The user behind a customer identity: a shared test customer, or the account created for the scenario. */
    public static TestUser customer(UserIdentity identity) {
        var account = identity.account();
        return account != null ? new TestUser(account.username(), account.password(), false)
                : customer(identity.customerIndex());
    }

    /** The user behind a customer or admin identity. */
    public static TestUser of(UserIdentity identity) {
        return switch (identity.kind()) {
            case CUSTOMER -> customer(identity);
            case ADMIN -> new TestUser(identity.account().username(), identity.account().password(), true);
            default -> throw new IllegalArgumentException("No user signs in as " + identity.kind());
        };
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /** Whether this user has the admin role. */
    public boolean isAdmin() {
        return admin;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TestUser user && username.equals(user.username) && password.equals(user.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

    @Override
    public String toString() {
        return username;
    }
}
