package com.mycompany.myshop.backend.support.core.scenario.then.steps;

import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;

/**
 * The navigation shared by every then-step: hop to another entity and keep asserting. Each entity
 * step reads its own state back through the SUT's API — the order via {@code GET /api/orders/{n}},
 * the coupons via {@code GET /api/coupons}, the history via {@code GET /api/orders}.
 */
public abstract class BaseThenStep<R, V extends ResponseVerification<R>> {

    protected final UseCaseDsl app;
    protected final ExecutionResultContext executionResult;
    protected final V successVerification;

    protected BaseThenStep(
            UseCaseDsl app, ExecutionResultContext executionResult, V successVerification) {
        this.app = app;
        this.executionResult = executionResult;
        this.successVerification = successVerification;
    }

    public ThenOrderImpl<R, V> order(String orderNumber) {
        return new ThenOrderImpl<>(app, executionResult, orderNumber, successVerification);
    }

    public ThenOrderImpl<R, V> order() {
        if (executionResult.getOrderNumber() == null) {
            throw new IllegalStateException(
                "Cannot verify the order: the executed action produced no order number. "
                    + "Name it explicitly with order(orderNumber).");
        }
        return order(executionResult.getOrderNumber());
    }

    public ThenCouponImpl<R, V> coupon(String couponCode) {
        return new ThenCouponImpl<>(app, executionResult, couponCode, successVerification);
    }

    public ThenCouponImpl<R, V> coupon() {
        if (executionResult.getCouponCode() == null) {
            throw new IllegalStateException(
                "Cannot verify the coupon: the executed action carried no coupon code. "
                    + "Name it explicitly with coupon(couponCode).");
        }
        return coupon(executionResult.getCouponCode());
    }

    public ThenOrderHistoryImpl<R, V> orderHistory() {
        return new ThenOrderHistoryImpl<>(app, executionResult, successVerification);
    }

    public ThenProductImpl<R, V> product(String sku) {
        return new ThenProductImpl<>(app, executionResult, sku, successVerification);
    }

    public ThenClockImpl<R, V> clock() {
        return new ThenClockImpl<>(app, executionResult, successVerification);
    }

    public ThenCountryImpl<R, V> country(String code) {
        return new ThenCountryImpl<>(app, executionResult, code, successVerification);
    }
}
