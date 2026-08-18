package com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.BrowseOrderHistoryVerification;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenOrderHistory;

public class ThenOrderHistoryImpl<R, V extends ResponseVerification<R>> extends BaseThenStep<R, V>
        implements ThenOrderHistory {

    private final BrowseOrderHistoryVerification verification;

    public ThenOrderHistoryImpl(
            UseCaseDsl app, ExecutionResultContext executionResult, V successVerification) {
        super(app, executionResult, successVerification);
        if (successVerification instanceof BrowseOrderHistoryVerification browseVerification) {
            this.verification = browseVerification;
        } else {
            this.verification = app.myShop().browseOrderHistory().execute().shouldSucceed();
        }
    }

    @Override
    public ThenOrderHistoryImpl<R, V> containsOrder(String expectedOrderNumber) {
        verification.hasOrderWithNumber(expectedOrderNumber);
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> containsOrder() {
        if (executionResult.getOrderNumber() == null) {
            throw new IllegalStateException(
                "Cannot verify the order history: the executed action produced no order number. "
                    + "Name it explicitly with containsOrder(orderNumber).");
        }
        return containsOrder(executionResult.getOrderNumber());
    }

    @Override
    public ThenOrderHistoryImpl<R, V> and() {
        return this;
    }
}
