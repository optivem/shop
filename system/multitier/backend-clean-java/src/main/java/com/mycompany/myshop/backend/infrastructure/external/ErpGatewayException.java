package com.mycompany.myshop.backend.infrastructure.external;

/** The ERP failed to answer. See {@link GatewayException}. */
public class ErpGatewayException extends GatewayException {

    public ErpGatewayException(String message) {
        super(message);
    }

    public ErpGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
