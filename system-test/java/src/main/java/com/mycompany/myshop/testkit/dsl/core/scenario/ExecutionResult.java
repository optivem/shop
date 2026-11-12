package com.mycompany.myshop.testkit.dsl.core.scenario;

import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import lombok.Getter;

@Getter
public class ExecutionResult<R, V extends ResponseVerification<R>> {
    private final UseCaseResult<R, V> result;
    private final ExecutionResultContext context;

    ExecutionResult(UseCaseResult<R, V> result, ExecutionResultContext context) {
        if (result == null) {
            throw new IllegalArgumentException("Result cannot be null");
        }
        this.result = result;
        this.context = context;
    }
}
