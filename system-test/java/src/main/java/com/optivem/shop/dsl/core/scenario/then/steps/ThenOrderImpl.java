package com.optivem.shop.dsl.core.scenario.then.steps;

import com.optivem.shop.dsl.core.shared.ResponseVerification;
import com.optivem.shop.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.dsl.core.scenario.ExecutionResultContext;
import com.optivem.shop.dsl.port.then.steps.ThenOrder;
import com.optivem.shop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.shop.dsl.core.usecase.shop.usecases.PlaceOrderVerification;
import com.optivem.shop.dsl.core.usecase.shop.usecases.ViewOrderVerification;

public class ThenOrderImpl<TSuccessResponse, TSuccessVerification extends ResponseVerification<TSuccessResponse>>
        extends BaseThenStep<TSuccessResponse, TSuccessVerification> implements ThenOrder {
    private final ViewOrderVerification orderVerification;

    public ThenOrderImpl(UseCaseDsl app, ExecutionResultContext executionResult, String orderNumber, TSuccessVerification successVerification) {
        super(app, executionResult, successVerification);
        if (orderNumber == null) {
            throw new IllegalStateException("Cannot verify order: no order number available from the executed operation");
        }
        if (successVerification instanceof ViewOrderVerification viewOrderVerification) {
            this.orderVerification = viewOrderVerification;
        } else {
            this.orderVerification = app.shop().viewOrder()
                    .orderNumber(orderNumber)
                    .execute()
                    .shouldSucceed();
        }
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasSku(String expectedSku) {
        orderVerification.sku(expectedSku);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasQuantity(int expectedQuantity) {
        orderVerification.quantity(expectedQuantity);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasUnitPrice(double expectedUnitPrice) {
        orderVerification.unitPrice(expectedUnitPrice);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasTotalPrice(double expectedTotalPrice) {
        orderVerification.totalPrice(expectedTotalPrice);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasStatus(OrderStatus expectedStatus) {
        orderVerification.status(expectedStatus);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasTotalPrice(String expectedTotalPrice) {
        orderVerification.totalPrice(expectedTotalPrice);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasTotalPriceGreaterThanZero() {
        orderVerification.totalPriceGreaterThanZero();
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasOrderNumberPrefix(String expectedPrefix) {
        switch (successVerification) {
            case PlaceOrderVerification placeOrderVerification -> placeOrderVerification.orderNumberStartsWith(expectedPrefix);
            case ViewOrderVerification viewOrderVerification -> viewOrderVerification.orderNumberHasPrefix(expectedPrefix);
            case null -> { /* no-op: orderNumber prefix not applicable */ }
            default -> { /* no-op: verification type has no order number prefix check */ }
        }
        orderVerification.orderNumberHasPrefix(expectedPrefix);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasBasePrice(double expectedBasePrice) {
        orderVerification.basePrice(expectedBasePrice);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasBasePrice(String basePrice) {
        orderVerification.basePrice(basePrice);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasSubtotalPrice(double expectedSubtotalPrice) {
        orderVerification.subtotalPrice(expectedSubtotalPrice);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasSubtotalPrice(String expectedSubtotalPrice) {
        orderVerification.subtotalPrice(expectedSubtotalPrice);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasDiscountAmount(double expectedDiscountAmount) {
        orderVerification.discountAmount(expectedDiscountAmount);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasDiscountAmount(String expectedDiscountAmount) {
        orderVerification.discountAmount(expectedDiscountAmount);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasAppliedCoupon(String expectedCouponCode) {
        orderVerification.appliedCouponCode(expectedCouponCode);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasAppliedCoupon() {
        return hasAppliedCoupon(executionResult.getCouponCode());
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasTaxRate(double expectedTaxRate) {
        orderVerification.taxRate(expectedTaxRate);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasTaxRate(String expectedTaxRate) {
        orderVerification.taxRate(expectedTaxRate);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasTaxAmount(String expectedTaxAmount) {
        orderVerification.taxAmount(expectedTaxAmount);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasDiscountRate(double expectedDiscountRate) {
        orderVerification.discountRate(expectedDiscountRate);
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> hasAppliedCouponCode(String expectedCouponCode) {
        return hasAppliedCoupon(expectedCouponCode);
    }

    @Override
    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> and() {
        return this;
    }
}
