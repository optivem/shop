package com.mycompany.myshop.backend.support.core.scenario;

import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.support.core.shared.UseCaseResult;

/** The outcome of the scenario's action, plus what it left behind for {@code then()}. */
public class ExecutionResult<R, V extends ResponseVerification<R>> {

    private final UseCaseResult<R, V> result;
    private final ExecutionResultContext context;

    ExecutionResult(UseCaseResult<R, V> result, String orderNumber, String couponCode) {
        if (result == null) {
            throw new IllegalArgumentException("Result cannot be null");
        }
        this.result = result;
        this.context = new ExecutionResultContext(orderNumber, couponCode);
    }

    public UseCaseResult<R, V> getResult() {
        return result;
    }

    public ExecutionResultContext getContext() {
        return context;
    }
}
