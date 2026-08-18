package com.mycompany.myshop.backend.infrastructure.external;

/** The tax system failed to answer. See {@link GatewayException}. */
public class TaxGatewayException extends GatewayException {

    public TaxGatewayException(String message) {
        super(message);
    }

    public TaxGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
