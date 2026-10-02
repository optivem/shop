package com.mycompany.myshop.testkit.driver.adapter.shared.client.http;

@FunctionalInterface
public interface BearerTokenSource {
    /** Returns the access token for the request, or null to send it without an Authorization header. */
    String tokenFor(String method, String path);
}
