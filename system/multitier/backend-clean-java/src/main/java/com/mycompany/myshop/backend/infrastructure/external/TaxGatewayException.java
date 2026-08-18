package com.mycompany.myshop.backend.infrastructure.external;

public class TaxGatewayException extends GatewayException {

    public TaxGatewayException(String message) {
        super(message);
    }

    public TaxGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
