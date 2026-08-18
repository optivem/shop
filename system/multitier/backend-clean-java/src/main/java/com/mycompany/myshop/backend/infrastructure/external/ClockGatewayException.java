package com.mycompany.myshop.backend.infrastructure.external;

/** The clock system failed to answer. See {@link GatewayException}. */
public class ClockGatewayException extends GatewayException {

    public ClockGatewayException(String message) {
        super(message);
    }

    public ClockGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
