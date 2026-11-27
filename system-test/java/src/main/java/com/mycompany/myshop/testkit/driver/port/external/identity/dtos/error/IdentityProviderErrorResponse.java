package com.mycompany.myshop.testkit.driver.port.external.identity.dtos.error;

public class IdentityProviderErrorResponse {
    private final String message;

    public IdentityProviderErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message != null ? message : "";
    }
}
