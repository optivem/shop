package com.mycompany.myshop.backend.support.core.shared;

/** Base for the per-use-case verifications: holds the parsed success payload to assert against. */
public class ResponseVerification<R> {

    private final R response;

    public ResponseVerification(R response) {
        this.response = response;
    }

    protected R getResponse() {
        return response;
    }
}
