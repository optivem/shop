package com.optivem.eshop.dsl.core.shared;

import com.optivem.eshop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.eshop.dsl.common.Result;

import java.util.function.BiFunction;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;

public class UseCaseResult<TSuccessResponse, TSuccessVerification> {
    private final Result<TSuccessResponse, ErrorResponse> result;
    private final UseCaseContext context;
    private final BiFunction<TSuccessResponse, UseCaseContext, TSuccessVerification> successVerificationFactory;

    public UseCaseResult(
            Result<TSuccessResponse, ErrorResponse> result,
            UseCaseContext context,
            BiFunction<TSuccessResponse, UseCaseContext, TSuccessVerification> successVerificationFactory) {
        this.result = result;
        this.context = context;
        this.successVerificationFactory = successVerificationFactory;
    }

    public TSuccessVerification shouldSucceed() {
        assertThatResult(result).isSuccess();
        return successVerificationFactory.apply(result.getValue(), context);
    }

    public ErrorVerification shouldFail() {
        assertThatResult(result).isFailure();
        return new ErrorVerification(result.getError(), context);
    }
}




