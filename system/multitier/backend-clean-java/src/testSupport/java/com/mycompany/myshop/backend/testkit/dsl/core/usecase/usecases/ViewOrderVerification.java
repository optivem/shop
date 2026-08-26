package com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.usecases.queries.order.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseVerification;

public class ViewOrderVerification extends ResponseVerification<ViewOrderDetailsResponse> {

    public ViewOrderVerification(ViewOrderDetailsResponse response) {
        super(response);
    }

    public ViewOrderVerification sku(String expectedSku) {
        assertThat(getResponse().sku()).as("sku").isEqualTo(expectedSku);
        return this;
    }

    public ViewOrderVerification quantity(int expectedQuantity) {
        assertThat(getResponse().quantity()).as("quantity").isEqualTo(expectedQuantity);
        return this;
    }

    public ViewOrderVerification unitPrice(String expectedUnitPrice) {
        assertThat(getResponse().unitPrice()).as("unit price").isEqualByComparingTo(expectedUnitPrice);
        return this;
    }

    public ViewOrderVerification basePrice(String expectedBasePrice) {
        assertThat(getResponse().basePrice()).as("base price").isEqualByComparingTo(expectedBasePrice);
        return this;
    }

    public ViewOrderVerification discountRate(String expectedDiscountRate) {
        assertThat(getResponse().discountRate())
            .as("discount rate")
            .isEqualByComparingTo(expectedDiscountRate);
        return this;
    }

    public ViewOrderVerification taxRate(String expectedTaxRate) {
        assertThat(getResponse().taxRate()).as("tax rate").isEqualByComparingTo(expectedTaxRate);
        return this;
    }

    public ViewOrderVerification orderNumberPrefix(String expectedPrefix) {
        assertThat(getResponse().orderNumber()).as("order number").startsWith(expectedPrefix);
        return this;
    }

    public ViewOrderVerification discountAmount(String expectedDiscountAmount) {
        assertThat(getResponse().discountAmount())
            .as("discount amount")
            .isEqualByComparingTo(expectedDiscountAmount);
        return this;
    }

    public ViewOrderVerification subtotalPrice(String expectedSubtotalPrice) {
        assertThat(getResponse().subtotalPrice())
            .as("subtotal price")
            .isEqualByComparingTo(expectedSubtotalPrice);
        return this;
    }

    public ViewOrderVerification taxAmount(String expectedTaxAmount) {
        assertThat(getResponse().taxAmount()).as("tax amount").isEqualByComparingTo(expectedTaxAmount);
        return this;
    }

    public ViewOrderVerification totalPrice(String expectedTotalPrice) {
        assertThat(getResponse().totalPrice()).as("total price").isEqualByComparingTo(expectedTotalPrice);
        return this;
    }

    public ViewOrderVerification status(OrderStatus expectedStatus) {
        // The read DTO carries status as a String since Chunk R — compare on the enum name, not
        // the enum, or the Object overload silently compares across types and never matches.
        assertThat(getResponse().status()).as("status").isEqualTo(expectedStatus.name());
        return this;
    }

    public ViewOrderVerification appliedCouponCode(String expectedCouponCode) {
        assertThat(getResponse().appliedCouponCode())
            .as("applied coupon code")
            .isEqualTo(expectedCouponCode);
        return this;
    }

    public ViewOrderVerification noAppliedCouponCode() {
        assertThat(getResponse().appliedCouponCode()).as("applied coupon code").isNull();
        return this;
    }
}
