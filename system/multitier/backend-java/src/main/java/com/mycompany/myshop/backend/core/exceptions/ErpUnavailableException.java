package com.mycompany.myshop.backend.core.exceptions;

/**
 * The ERP could not answer: it is unreachable, timed out, or returned an error status.
 * A product the ERP reports as missing (404) is not this; that is a validation failure.
 */
public class ErpUnavailableException extends RuntimeException {
    public ErpUnavailableException(String message) {
        super(message);
    }

    public ErpUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
