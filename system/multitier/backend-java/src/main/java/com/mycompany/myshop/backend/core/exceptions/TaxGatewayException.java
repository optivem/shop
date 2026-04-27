package com.mycompany.myshop.backend.core.exceptions;

public class TaxGatewayException extends RuntimeException {
    public TaxGatewayException(String message) {
        super(message);
    }

    public TaxGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
