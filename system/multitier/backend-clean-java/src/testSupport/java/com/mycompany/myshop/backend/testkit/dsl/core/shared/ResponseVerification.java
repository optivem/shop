package com.mycompany.myshop.backend.testkit.dsl.core.shared;

public class ResponseVerification<R> {

    private final R response;

    public ResponseVerification(R response) {
        this.response = response;
    }

    protected R getResponse() {
        return response;
    }
}
