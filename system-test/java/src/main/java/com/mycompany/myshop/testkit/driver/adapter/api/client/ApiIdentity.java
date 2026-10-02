package com.mycompany.myshop.testkit.driver.adapter.api.client;

public enum ApiIdentity {
    /** Admin for admin-only operations, customer otherwise. */
    DEFAULT,
    /** Call without any Authorization header. */
    ANONYMOUS,
    CUSTOMER,
    ADMIN
}
