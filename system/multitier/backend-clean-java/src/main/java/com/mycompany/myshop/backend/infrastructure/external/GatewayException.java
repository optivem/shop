package com.mycompany.myshop.backend.infrastructure.external;

/**
 * An external system failed to answer: a non-2xx status it was not supposed to return, an IO
 * failure, or an interrupt while waiting. One type per gateway, all of them under this base, so the
 * whole failure class can be caught in one place.
 *
 * <p>Deliberately <em>not</em> an {@link IllegalStateException}. That is the JDK's programmer-error
 * signal, and a network timeout is not a programmer error — conflating the two makes an
 * infrastructure failure indistinguishable from a bug in our own code.
 */
public abstract class GatewayException extends RuntimeException {

    protected GatewayException(String message) {
        super(message);
    }

    protected GatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
