package com.mycompany.myshop.testkit.dsl.core.shared;

import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;
import com.mycompany.myshop.testkit.common.Result;

import java.util.function.BiFunction;

import static com.mycompany.myshop.testkit.common.ResultAssert.assertThatResult;

public class UseCaseResult<R, V> {
    private final Result<R, SystemError> result;
    private final UseCaseContext context;
    private final BiFunction<R, UseCaseContext, V> successVerificationFactory;

    public UseCaseResult(
            Result<R, SystemError> result,
            UseCaseContext context,
            BiFunction<R, UseCaseContext, V> successVerificationFactory) {
        this.result = result;
        this.context = context;
        this.successVerificationFactory = successVerificationFactory;
    }

    public V shouldSucceed() {
        assertThatResult(result).isSuccess();
        return successVerificationFactory.apply(result.getValue(), context);
    }

    /**
     * Asserts the operation succeeded and returns its response as is, for lookups the scenario layer needs (such as
     * which product an order is for); assertions go through {@link #shouldSucceed()}.
     */
    public R successResponse() {
        assertThatResult(result).isSuccess();
        return result.getValue();
    }

    public ErrorVerification shouldFail() {
        assertThatResult(result).isFailure();
        return new ErrorVerification(result.getError(), context);
    }
}
