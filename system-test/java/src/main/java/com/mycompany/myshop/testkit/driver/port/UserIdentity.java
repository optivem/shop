package com.mycompany.myshop.testkit.driver.port;

/**
 * Who an operation is performed as. A shared test customer carries its zero-based position (0 = customer1,
 * 1 = customer2, ...); a customer created for the scenario carries its account instead. Two identities are the
 * same user exactly when they are equal.
 */
public record UserIdentity(Kind kind, int customerIndex, CustomerAccount account) {
    /** The number of shared test customers in the identity provider (customer1, customer2). */
    public static final int CUSTOMER_COUNT = 2;

    public enum Kind {
        DEFAULT,
        ANONYMOUS,
        CUSTOMER,
        ADMIN
    }

    /** Customer for customer operations, admin for admin-only operations. */
    public static final UserIdentity DEFAULT = new UserIdentity(Kind.DEFAULT, -1);
    /** No credentials at all. */
    public static final UserIdentity ANONYMOUS = new UserIdentity(Kind.ANONYMOUS, -1);
    public static final UserIdentity ADMIN = new UserIdentity(Kind.ADMIN, -1);

    public UserIdentity(Kind kind, int customerIndex) {
        this(kind, customerIndex, null);
    }

    /**
     * The sign-in username of this customer identity (customer1, customer2, ..., or the created account's), as the
     * identity provider's test realm knows it; used where the system takes a customer by username (the admin
     * customer filter).
     */
    public String username() {
        if (kind != Kind.CUSTOMER) {
            throw new IllegalStateException("Only a customer identity has a customer username; this is " + kind);
        }
        return account != null ? account.username() : "customer" + (customerIndex + 1);
    }

    /** The shared test customer at the given zero-based position. */
    public static UserIdentity customer(int customerIndex) {
        if (customerIndex < 0 || customerIndex >= CUSTOMER_COUNT) {
            throw new IllegalArgumentException("Test customer index " + customerIndex
                    + " is out of range; there are " + CUSTOMER_COUNT + " test customers");
        }
        return new UserIdentity(Kind.CUSTOMER, customerIndex);
    }

    /** A customer whose account was created for this scenario. */
    public static UserIdentity customer(CustomerAccount account) {
        if (account == null) {
            throw new IllegalArgumentException("A created customer needs its account");
        }
        return new UserIdentity(Kind.CUSTOMER, -1, account);
    }
}
