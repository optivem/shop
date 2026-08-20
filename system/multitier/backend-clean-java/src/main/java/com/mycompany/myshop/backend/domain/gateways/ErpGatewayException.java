package com.mycompany.myshop.backend.domain.gateways;

public class ErpGatewayException extends GatewayException {

    public ErpGatewayException(String message) {
        super(message);
    }

    public ErpGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
