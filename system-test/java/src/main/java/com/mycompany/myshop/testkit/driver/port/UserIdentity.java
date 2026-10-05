package com.mycompany.myshop.testkit.driver.port;

public enum UserIdentity {
    /** Customer for customer operations, admin for admin-only operations. */
    DEFAULT,
    /** No credentials at all. */
    ANONYMOUS,
    CUSTOMER,
    /** A second customer, distinct from {@link #CUSTOMER}. */
    OTHER_CUSTOMER,
    ADMIN
}
