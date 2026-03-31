package com.optivem.eshop.dsl.core.scenario;

import com.optivem.eshop.dsl.core.shared.UseCaseResult;
import com.optivem.eshop.dsl.core.shared.ResponseVerification;

public class ExecutionResultBuilder<TSuccessResponse, TSuccessVerification extends ResponseVerification<TSuccessResponse>> {
    private final UseCaseResult<TSuccessResponse, TSuccessVerification> result;
    private String orderNumber;

    public ExecutionResultBuilder(UseCaseResult<TSuccessResponse, TSuccessVerification> result) {
        this.result = result;
    }

    public ExecutionResultBuilder<TSuccessResponse, TSuccessVerification> orderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    public ExecutionResult<TSuccessResponse, TSuccessVerification> build() {
        return new ExecutionResult<>(result, orderNumber);
    }
}



