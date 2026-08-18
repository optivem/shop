package com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.testkit.common.Converter;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.ViewOrderVerification;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenOrder;

public class ThenOrderImpl<R, V extends ResponseVerification<R>> extends BaseThenStep<R, V>
        implements ThenOrder {

    private final ViewOrderVerification orderVerification;

    public ThenOrderImpl(
            UseCaseDsl app,
            ExecutionResultContext executionResult,
            String orderNumber,
            V successVerification) {
        super(app, executionResult, successVerification);
        if (orderNumber == null) {
            throw new IllegalStateException("Cannot verify the order: no order number available");
        }
        if (successVerification instanceof ViewOrderVerification viewOrderVerification) {
            this.orderVerification = viewOrderVerification;
        } else {
            this.orderVerification =
                app.myShop().viewOrder().orderNumber(orderNumber).execute().shouldSucceed();
        }
    }

    @Override
    public ThenOrderImpl<R, V> hasSku(String expectedSku) {
        orderVerification.sku(expectedSku);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasQuantity(int expectedQuantity) {
        orderVerification.quantity(expectedQuantity);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasUnitPrice(String expectedUnitPrice) {
        orderVerification.unitPrice(expectedUnitPrice);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasUnitPrice(double expectedUnitPrice) {
        return hasUnitPrice(Converter.fromDouble(expectedUnitPrice));
    }

    @Override
    public ThenOrderImpl<R, V> hasBasePrice(String expectedBasePrice) {
        orderVerification.basePrice(expectedBasePrice);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasBasePrice(double expectedBasePrice) {
        return hasBasePrice(Converter.fromDouble(expectedBasePrice));
    }

    @Override
    public ThenOrderImpl<R, V> hasDiscountRate(String expectedDiscountRate) {
        orderVerification.discountRate(expectedDiscountRate);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasDiscountRate(double expectedDiscountRate) {
        return hasDiscountRate(Converter.fromDouble(expectedDiscountRate));
    }

    @Override
    public ThenOrderImpl<R, V> hasTaxRate(String expectedTaxRate) {
        orderVerification.taxRate(expectedTaxRate);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasTaxRate(double expectedTaxRate) {
        return hasTaxRate(Converter.fromDouble(expectedTaxRate));
    }

    @Override
    public ThenOrderImpl<R, V> hasOrderNumberPrefix(String expectedPrefix) {
        orderVerification.orderNumberPrefix(expectedPrefix);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasDiscountAmount(String expectedDiscountAmount) {
        orderVerification.discountAmount(expectedDiscountAmount);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasDiscountAmount(double expectedDiscountAmount) {
        return hasDiscountAmount(Converter.fromDouble(expectedDiscountAmount));
    }

    @Override
    public ThenOrderImpl<R, V> hasSubtotalPrice(String expectedSubtotalPrice) {
        orderVerification.subtotalPrice(expectedSubtotalPrice);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasSubtotalPrice(double expectedSubtotalPrice) {
        return hasSubtotalPrice(Converter.fromDouble(expectedSubtotalPrice));
    }

    @Override
    public ThenOrderImpl<R, V> hasTaxAmount(String expectedTaxAmount) {
        orderVerification.taxAmount(expectedTaxAmount);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasTaxAmount(double expectedTaxAmount) {
        return hasTaxAmount(Converter.fromDouble(expectedTaxAmount));
    }

    @Override
    public ThenOrderImpl<R, V> hasTotalPrice(String expectedTotalPrice) {
        orderVerification.totalPrice(expectedTotalPrice);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasTotalPrice(double expectedTotalPrice) {
        return hasTotalPrice(Converter.fromDouble(expectedTotalPrice));
    }

    @Override
    public ThenOrderImpl<R, V> hasStatus(OrderStatus expectedStatus) {
        orderVerification.status(expectedStatus);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasAppliedCoupon(String expectedCouponCode) {
        orderVerification.appliedCouponCode(expectedCouponCode);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasAppliedCoupon() {
        return hasAppliedCoupon(executionResult.getCouponCode());
    }

    @Override
    public ThenOrderImpl<R, V> hasNoAppliedCoupon() {
        orderVerification.noAppliedCouponCode();
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> and() {
        return this;
    }
}
