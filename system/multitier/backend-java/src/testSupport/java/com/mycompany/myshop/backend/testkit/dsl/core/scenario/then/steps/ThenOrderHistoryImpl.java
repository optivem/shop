package com.mycompany.myshop.backend.support.core.scenario.then.steps;

import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.core.usecase.usecases.BrowseOrderHistoryVerification;
import com.mycompany.myshop.backend.support.port.then.steps.ThenOrderHistory;

/**
 * Order history. When the action <em>was</em> a browse, its response is asserted directly; otherwise
 * the history is read back through {@code GET /api/orders} — so {@code when().placeOrder().then()
 * .shouldSucceed().and().orderHistory().containsOrder()} states the whole story as one scenario.
 */
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
