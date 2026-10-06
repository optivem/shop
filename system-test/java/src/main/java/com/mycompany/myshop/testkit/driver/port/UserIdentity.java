package com.mycompany.myshop.testkit.driver.port;

/**
 * Who an operation is performed as. A customer identity carries the zero-based position of the test customer
 * (0 = customer1, 1 = customer2, ...), so two identities are the same user exactly when they are equal.
 */
public record UserIdentity(Kind kind, int customerIndex) {
    /** The number of test customers available in the identity provider (customer1, customer2). */
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

    /** The test customer at the given zero-based position. */
    public static UserIdentity customer(int customerIndex) {
        if (customerIndex < 0 || customerIndex >= CUSTOMER_COUNT) {
            throw new IllegalArgumentException("Test customer index " + customerIndex
                    + " is out of range; there are " + CUSTOMER_COUNT + " test customers");
        }
        return new UserIdentity(Kind.CUSTOMER, customerIndex);
    }
}
