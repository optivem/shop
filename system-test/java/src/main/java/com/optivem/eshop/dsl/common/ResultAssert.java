package com.optivem.eshop.dsl.common;

import org.assertj.core.api.AbstractAssert;

public class ResultAssert<T, E> extends AbstractAssert<ResultAssert<T, E>, Result<T, E>> {
    private ResultAssert(Result<T, E> actual) {
        super(actual, ResultAssert.class);
    }

    public static <T, E> ResultAssert<T, E> assertThatResult(Result<T, E> actual) {
        return new ResultAssert<>(actual);
    }

    public ResultAssert<T, E> isSuccess() {
        isNotNull();
        if (!actual.isSuccess()) {
            failWithMessage("Expected result to be success but was failure with error: %s", actual.getError());
        }
        return this;
    }

    public ResultAssert<T, E> isFailure() {
        isNotNull();
        if (!actual.isFailure()) {
            failWithMessage("Expected result to be failure but was success");
        }
        return this;
    }
}


