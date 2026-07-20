package com.mycompany.myshop.backend.support.core.scenario.then.steps;

import com.mycompany.myshop.backend.core.entities.OrderStatus;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.core.usecase.usecases.ViewOrderVerification;
import com.mycompany.myshop.backend.support.port.then.steps.ThenOrder;
import java.math.BigDecimal;

/**
 * The persisted order. When the action <em>was</em> a view-order, its response is asserted directly;
 * otherwise the order is read back through {@code GET /api/orders/{orderNumber}} — which is what
 * makes {@code when().placeOrder().then().shouldSucceed().and().order().hasTotalPrice(...)} check
 * what the system actually stored, not what the place-order response happened to echo.
 */
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
    public ThenOrderImpl<R, V> hasBasePrice(String expectedBasePrice) {
        orderVerification.basePrice(expectedBasePrice);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasDiscountRate(String expectedDiscountRate) {
        orderVerification.discountRate(expectedDiscountRate);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasDiscountRate(double expectedDiscountRate) {
        return hasDiscountRate(BigDecimal.valueOf(expectedDiscountRate).toPlainString());
    }

    @Override
    public ThenOrderImpl<R, V> hasTaxRate(String expectedTaxRate) {
        orderVerification.taxRate(expectedTaxRate);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasTaxRate(double expectedTaxRate) {
        return hasTaxRate(BigDecimal.valueOf(expectedTaxRate).toPlainString());
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
    public ThenOrderImpl<R, V> hasSubtotalPrice(String expectedSubtotalPrice) {
        orderVerification.subtotalPrice(expectedSubtotalPrice);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasTaxAmount(String expectedTaxAmount) {
        orderVerification.taxAmount(expectedTaxAmount);
        return this;
    }

    @Override
    public ThenOrderImpl<R, V> hasTotalPrice(String expectedTotalPrice) {
        orderVerification.totalPrice(expectedTotalPrice);
        return this;
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
