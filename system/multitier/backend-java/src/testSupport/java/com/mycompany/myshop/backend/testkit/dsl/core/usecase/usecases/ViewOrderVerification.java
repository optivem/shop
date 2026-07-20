package com.mycompany.myshop.backend.support.core.usecase.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.core.dtos.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.core.entities.OrderStatus;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;

/**
 * The persisted order. Money is asserted with {@code isEqualByComparingTo} against a {@code String}
 * literal, so {@code "20.00"} and {@code 20.0} are the same amount and the test states the figure
 * the way the domain writes it.
 */
public class ViewOrderVerification extends ResponseVerification<ViewOrderDetailsResponse> {

    public ViewOrderVerification(ViewOrderDetailsResponse response) {
        super(response);
    }

    public ViewOrderVerification sku(String expectedSku) {
        assertThat(getResponse().getSku()).as("sku").isEqualTo(expectedSku);
        return this;
    }

    public ViewOrderVerification quantity(int expectedQuantity) {
        assertThat(getResponse().getQuantity()).as("quantity").isEqualTo(expectedQuantity);
        return this;
    }

    public ViewOrderVerification unitPrice(String expectedUnitPrice) {
        assertThat(getResponse().getUnitPrice()).as("unit price").isEqualByComparingTo(expectedUnitPrice);
        return this;
    }

    public ViewOrderVerification basePrice(String expectedBasePrice) {
        assertThat(getResponse().getBasePrice()).as("base price").isEqualByComparingTo(expectedBasePrice);
        return this;
    }

    public ViewOrderVerification discountRate(String expectedDiscountRate) {
        assertThat(getResponse().getDiscountRate())
            .as("discount rate")
            .isEqualByComparingTo(expectedDiscountRate);
        return this;
    }

    public ViewOrderVerification taxRate(String expectedTaxRate) {
        assertThat(getResponse().getTaxRate()).as("tax rate").isEqualByComparingTo(expectedTaxRate);
        return this;
    }

    public ViewOrderVerification orderNumberPrefix(String expectedPrefix) {
        assertThat(getResponse().getOrderNumber()).as("order number").startsWith(expectedPrefix);
        return this;
    }

    public ViewOrderVerification discountAmount(String expectedDiscountAmount) {
        assertThat(getResponse().getDiscountAmount())
            .as("discount amount")
            .isEqualByComparingTo(expectedDiscountAmount);
        return this;
    }

    public ViewOrderVerification subtotalPrice(String expectedSubtotalPrice) {
        assertThat(getResponse().getSubtotalPrice())
            .as("subtotal price")
            .isEqualByComparingTo(expectedSubtotalPrice);
        return this;
    }

    public ViewOrderVerification taxAmount(String expectedTaxAmount) {
        assertThat(getResponse().getTaxAmount()).as("tax amount").isEqualByComparingTo(expectedTaxAmount);
        return this;
    }

    public ViewOrderVerification totalPrice(String expectedTotalPrice) {
        assertThat(getResponse().getTotalPrice()).as("total price").isEqualByComparingTo(expectedTotalPrice);
        return this;
    }

    public ViewOrderVerification status(OrderStatus expectedStatus) {
        assertThat(getResponse().getStatus()).as("status").isEqualTo(expectedStatus);
        return this;
    }

    public ViewOrderVerification appliedCouponCode(String expectedCouponCode) {
        assertThat(getResponse().getAppliedCouponCode())
            .as("applied coupon code")
            .isEqualTo(expectedCouponCode);
        return this;
    }

    public ViewOrderVerification noAppliedCouponCode() {
        assertThat(getResponse().getAppliedCouponCode()).as("applied coupon code").isNull();
        return this;
    }
}
