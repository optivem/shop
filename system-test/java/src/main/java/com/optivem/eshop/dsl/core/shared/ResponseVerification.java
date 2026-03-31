package com.optivem.eshop.dsl.core.shared;

public class ResponseVerification<TResponse> {
    private final TResponse response;
    private final UseCaseContext context;

    public ResponseVerification(TResponse response, UseCaseContext context) {
        this.response = response;
        this.context = context;
    }

    protected TResponse getResponse() {
        return response;
    }

    protected UseCaseContext getContext() {
        return context;
    }
}



