package com.optivem.shop.testkit.dsl.core.scenario;

import com.optivem.shop.testkit.dsl.core.shared.UseCaseResult;
import com.optivem.shop.testkit.dsl.core.shared.ResponseVerification;

public class ExecutionResultBuilder<TSuccessResponse, TSuccessVerification extends ResponseVerification<TSuccessResponse>> {
    private final UseCaseResult<TSuccessResponse, TSuccessVerification> result;
    private String orderNumber;
    private String couponCode;

    public ExecutionResultBuilder(UseCaseResult<TSuccessResponse, TSuccessVerification> result) {
        this.result = result;
    }

    public ExecutionResultBuilder<TSuccessResponse, TSuccessVerification> orderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    public ExecutionResultBuilder<TSuccessResponse, TSuccessVerification> couponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    public ExecutionResult<TSuccessResponse, TSuccessVerification> build() {
        return new ExecutionResult<>(result, orderNumber, couponCode);
    }
}
