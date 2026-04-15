package com.optivem.shop.testkit.dsl.core.scenario;

import com.optivem.shop.testkit.dsl.core.shared.UseCaseResult;
import com.optivem.shop.testkit.dsl.core.shared.ResponseVerification;
import lombok.Getter;

@Getter
public class ExecutionResult<TSuccessResponse, TSuccessVerification extends ResponseVerification<TSuccessResponse>> {
    private final UseCaseResult<TSuccessResponse, TSuccessVerification> result;
    private final ExecutionResultContext context;

    ExecutionResult(UseCaseResult<TSuccessResponse, TSuccessVerification> result, String orderNumber, String couponCode) {
        if (result == null) {
            throw new IllegalArgumentException("Result cannot be null");
        }
        this.result = result;
        this.context = new ExecutionResultContext(orderNumber, couponCode);
    }
}
