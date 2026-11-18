package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.BrowseCustomersVerification;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenCustomerList;

import java.util.Arrays;

public class ThenCustomerListImpl<R, V extends ResponseVerification<R>>
        extends BaseThenStep<R, V> implements ThenCustomerList {
    private final BrowseCustomersVerification verification;

    public ThenCustomerListImpl(UseCaseDsl app, ExecutionResultContext executionResult, V successVerification) {
        super(app, executionResult, successVerification);
        if (!(successVerification instanceof BrowseCustomersVerification browseVerification)) {
            throw new IllegalStateException("Cannot verify the customer list: the executed operation did not browse the customers");
        }
        this.verification = browseVerification;
    }

    @Override
    public ThenCustomerListImpl<R, V> listsCustomersInOrder(String... customerAliases) {
        verification.listsCustomersInOrder(Arrays.stream(customerAliases).map(executionResult::customerUsername).toList());
        return this;
    }

    @Override
    public ThenCustomerListImpl<R, V> and() {
        return this;
    }
}
