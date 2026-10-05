package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.BrowseOrderHistoryVerification;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenOrderHistory;

public class ThenOrderHistoryImpl<R, V extends ResponseVerification<R>>
        extends BaseThenStep<R, V> implements ThenOrderHistory {
    private final BrowseOrderHistoryVerification verification;

    public ThenOrderHistoryImpl(UseCaseDsl app, ExecutionResultContext executionResult, V successVerification) {
        super(app, executionResult, successVerification);
        if (!(successVerification instanceof BrowseOrderHistoryVerification browseVerification)) {
            throw new IllegalStateException("Cannot verify order history: the executed operation did not browse the order history");
        }
        this.verification = browseVerification;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> containsOrder(String orderNumber) {
        verification.containsOrder(orderNumber);
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> doesNotContainOrder(String orderNumber) {
        verification.doesNotContainOrder(orderNumber);
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> and() {
        return this;
    }
}
