package com.mycompany.myshop.testkit.dsl.core.scenario;

import java.util.function.UnaryOperator;

import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;

public class ExecutionResultBuilder<R, V extends ResponseVerification<R>> {
    private final UseCaseResult<R, V> result;
    private String orderNumber;
    private String couponCode;
    private UnaryOperator<String> customerUsernames;

    public ExecutionResultBuilder(UseCaseResult<R, V> result) {
        this.result = result;
    }

    public ExecutionResultBuilder<R, V> orderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    public ExecutionResultBuilder<R, V> couponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    /** Resolves the scenario's customer aliases to usernames, for assertions that name customers. */
    public ExecutionResultBuilder<R, V> customerUsernames(UnaryOperator<String> customerUsernames) {
        this.customerUsernames = customerUsernames;
        return this;
    }

    public ExecutionResult<R, V> build() {
        return new ExecutionResult<>(result, new ExecutionResultContext(orderNumber, couponCode, customerUsernames));
    }
}
