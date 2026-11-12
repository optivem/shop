package com.mycompany.myshop.testkit.driver.adapter.shared.client.http;

import com.mycompany.myshop.testkit.driver.port.UserIdentity;

import java.util.Objects;

/**
 * Credentials the drivers sign in with: the test realm's shared users, or a customer account created for the
 * scenario. The shared users are singletons, so they can be compared with {@code ==}.
 */
public final class TestUser {
    public static final TestUser CUSTOMER = new TestUser("customer1", "customer1-test-password");
    public static final TestUser CUSTOMER2 = new TestUser("customer2", "customer2-test-password");
    public static final TestUser ADMIN = new TestUser("admin1", "admin1-test-password");

    private final String username;
    private final String password;

    private TestUser(String username, String password) {
        this.username = username;
        this.password = password;
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
        return account != null ? new TestUser(account.username(), account.password()) : customer(identity.customerIndex());
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
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
