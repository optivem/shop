package com.mycompany.myshop.testkit.driver.port.external.identity;

import com.mycompany.myshop.testkit.driver.port.UserAccount;

/**
 * The identity provider's test realm, for scenarios that need a user of their own: a customer (no saved profile, no
 * orders) or an admin. Unlike the system drivers it arranges, never asserts, so a failure is thrown rather than
 * returned. Closing it removes every account it created.
 */
public interface IdentityProviderDriver extends AutoCloseable {
    /**
     * Creates a new customer sign-in account with this name and email, and the customer role.
     *
     * @param email null for a unique address derived from the generated username
     */
    UserAccount createCustomer(String firstName, String lastName, String email);

    /**
     * Creates a new admin sign-in account with this name and email, and the admin role.
     *
     * @param email null for a unique address derived from the generated username
     */
    UserAccount createAdmin(String firstName, String lastName, String email);

    /**
     * A new unique username and password for a customer account that is created elsewhere (a visitor's sign-up,
     * UC-013); call {@link #deleteOnClose(String)} once it exists.
     */
    UserAccount newAccountCredentials();

    /** A unique email for an account from {@link #newAccountCredentials()}. */
    String newAccountEmail(UserAccount account);

    /** Removes the account with this username on close, like the ones this driver created. */
    void deleteOnClose(String username);

    @Override
    void close();
}
