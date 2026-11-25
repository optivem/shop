package com.mycompany.myshop.testkit.driver.adapter.api.client;

/** Who the API client calls as, short of a given user ({@link MyShopApiClient#asUser}). */
public enum ApiIdentity {
    /** Call without any Authorization header. */
    ANONYMOUS,
    /** The shared test customer customer1. */
    CUSTOMER
}
