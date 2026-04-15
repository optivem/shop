package com.optivem.shop.testkit.dsl.core.usecase.shop.usecases;

import com.optivem.shop.testkit.driver.port.shop.dtos.ViewOrderResponse;
import com.optivem.shop.testkit.driver.port.shop.dtos.OrderStatus;
import com.optivem.shop.testkit.dsl.core.shared.ResponseVerification;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseContext;
import com.optivem.shop.testkit.common.Converter;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class ViewOrderVerification extends ResponseVerification<ViewOrderResponse> {
    public ViewOrderVerification(ViewOrderResponse response, UseCaseContext context) {
        super(response, context);
    }

    public ViewOrderVerification orderNumber(String orderNumberResultAlias) {
        var expectedOrderNumber = getContext().getResultValue(orderNumberResultAlias);
        var actualOrderNumber = getResponse().getOrderNumber();
        assertThat(actualOrderNumber)
                .withFailMessage("Expected order number to be '%s', but was '%s'", expectedOrderNumber, actualOrderNumber)
                .isEqualTo(expectedOrderNumber);
        return this;
    }

    public ViewOrderVerification sku(String skuParamAlias) {
        var expectedSku = getContext().getParamValue(skuParamAlias);
        var actualSku = getResponse().getSku();
        assertThat(actualSku)
                .withFailMessage("Expected SKU to be '%s', but was '%s'", expectedSku, actualSku)
                .isEqualTo(expectedSku);
        return this;
    }

    public ViewOrderVerification quantity(int expectedQuantity) {
        var actualQuantity = getResponse().getQuantity();
        assertThat(actualQuantity)
                .withFailMessage("Expected quantity to be %d, but was %d", expectedQuantity, actualQuantity)
                .isEqualTo(expectedQuantity);
        return this;
    }

    public ViewOrderVerification quantity(String expectedQuantity) {
        return quantity(Converter.toInteger(expectedQuantity));
    }

    public ViewOrderVerification unitPrice(double expectedUnitPrice) {
        var expectedPrice = Converter.toBigDecimal(expectedUnitPrice);
        var actualPrice = getResponse().getUnitPrice();
        assertThat(actualPrice)
                .withFailMessage("Expected unit price to be %s, but was %s", expectedPrice, actualPrice)
                .isEqualByComparingTo(expectedPrice);
        return this;
    }

    public ViewOrderVerification unitPrice(String expectedUnitPrice) {
        return unitPrice(Converter.toDouble(expectedUnitPrice));
    }

    public ViewOrderVerification status(OrderStatus expectedStatus) {
        var actualStatus = getResponse().getStatus();
        assertThat(actualStatus)
                .withFailMessage("Expected status to be %s, but was %s", expectedStatus, actualStatus)
                .isEqualTo(expectedStatus);
        return this;
    }

    public ViewOrderVerification status(String expectedStatus) {
        return status(OrderStatus.valueOf(expectedStatus));
    }

    public ViewOrderVerification totalPrice(BigDecimal expectedTotalPrice) {
        var totalPrice = getResponse().getTotalPrice();
        assertThat(totalPrice)
                .withFailMessage("Expected total price to be %s, but was %s", expectedTotalPrice, totalPrice)
                .isEqualByComparingTo(expectedTotalPrice);
        return this;
    }

    public ViewOrderVerification totalPrice(double expectedTotalPrice) {
        return totalPrice(Converter.toBigDecimal(expectedTotalPrice));
    }

    public ViewOrderVerification totalPrice(String expectedTotalPrice) {
        return totalPrice(Converter.toBigDecimal(expectedTotalPrice));
    }

    public ViewOrderVerification totalPriceGreaterThanZero() {
        var totalPrice = getResponse().getTotalPrice();
        assertThat(totalPrice)
                .withFailMessage("Total price should be positive, but was: %s", totalPrice)
                .isGreaterThan(BigDecimal.ZERO);
        return this;
    }

    public ViewOrderVerification subtotalPrice(BigDecimal expectedSubtotalPrice) {
        var actualSubtotalPrice = getResponse().getSubtotalPrice();
        assertThat(actualSubtotalPrice)
                .withFailMessage("Expected subtotal price to be %s, but was %s", expectedSubtotalPrice, actualSubtotalPrice)
                .isEqualByComparingTo(expectedSubtotalPrice);
        return this;
    }

    public ViewOrderVerification subtotalPrice(double expectedSubtotalPrice) {
        return subtotalPrice(Converter.toBigDecimal(expectedSubtotalPrice));
    }

    public ViewOrderVerification subtotalPrice(String expectedSubtotalPrice) {
        return subtotalPrice(Converter.toDouble(expectedSubtotalPrice));
    }

    public ViewOrderVerification basePrice(BigDecimal expectedBasePrice) {
        var actualBasePrice = getResponse().getBasePrice();
        assertThat(actualBasePrice)
                .withFailMessage("Expected base price to be %s, but was %s", expectedBasePrice, actualBasePrice)
                .isEqualByComparingTo(expectedBasePrice);
        return this;
    }

    public ViewOrderVerification basePrice(double expectedBasePrice) {
        return basePrice(Converter.toBigDecimal(expectedBasePrice));
    }

    public ViewOrderVerification basePrice(String expectedBasePrice) {
        return basePrice(Converter.toBigDecimal(expectedBasePrice));
    }

    public ViewOrderVerification discountAmount(BigDecimal expectedDiscountAmount) {
        var actualDiscountAmount = getResponse().getDiscountAmount();
        assertThat(actualDiscountAmount)
                .withFailMessage("Expected discount amount to be %s, but was %s", expectedDiscountAmount, actualDiscountAmount)
                .isEqualByComparingTo(expectedDiscountAmount);
        return this;
    }

    public ViewOrderVerification discountAmount(double expectedDiscountAmount) {
        return discountAmount(Converter.toBigDecimal(expectedDiscountAmount));
    }

    public ViewOrderVerification discountAmount(String expectedDiscountAmount) {
        return discountAmount(Converter.toBigDecimal(expectedDiscountAmount));
    }

    public ViewOrderVerification taxRate(BigDecimal expectedTaxRate) {
        var actualTaxRate = getResponse().getTaxRate();
        assertThat(actualTaxRate)
                .withFailMessage("Expected tax rate to be %s, but was %s", expectedTaxRate, actualTaxRate)
                .isEqualByComparingTo(expectedTaxRate);
        return this;
    }

    public ViewOrderVerification taxRate(double expectedTaxRate) {
        return taxRate(Converter.toBigDecimal(expectedTaxRate));
    }

    public ViewOrderVerification taxRate(String expectedTaxRate) {
        return taxRate(Converter.toBigDecimal(expectedTaxRate));
    }

    public ViewOrderVerification taxAmount(BigDecimal expectedTaxAmount) {
        var actualTaxAmount = getResponse().getTaxAmount();
        assertThat(actualTaxAmount)
                .withFailMessage("Expected tax amount to be %s, but was %s", expectedTaxAmount, actualTaxAmount)
                .isEqualByComparingTo(expectedTaxAmount);
        return this;
    }

    public ViewOrderVerification taxAmount(double expectedTaxAmount) {
        return taxAmount(Converter.toBigDecimal(expectedTaxAmount));
    }

    public ViewOrderVerification taxAmount(String expectedTaxAmount) {
        return taxAmount(Converter.toBigDecimal(expectedTaxAmount));
    }

    public ViewOrderVerification discountRate(BigDecimal expectedDiscountRate) {
        var actualDiscountRate = getResponse().getDiscountRate();
        assertThat(actualDiscountRate)
                .withFailMessage("Expected discount rate to be %s, but was %s", expectedDiscountRate, actualDiscountRate)
                .isEqualByComparingTo(expectedDiscountRate);
        return this;
    }

    public ViewOrderVerification discountRate(double expectedDiscountRate) {
        return discountRate(Converter.toBigDecimal(expectedDiscountRate));
    }

    public ViewOrderVerification appliedCouponCode(String expectedCouponCodeAlias) {
        var expectedCouponCode = getContext().getParamValue(expectedCouponCodeAlias);
        var actualCouponCode = getResponse().getAppliedCouponCode();
        assertThat(actualCouponCode)
                .withFailMessage("Expected applied coupon code to be '%s', but was '%s'", expectedCouponCode, actualCouponCode)
                .isEqualTo(expectedCouponCode);
        return this;
    }

    public void orderNumberHasPrefix(String expectedPrefix) {
        var actualOrderNumber = getResponse().getOrderNumber();
        assertThat(actualOrderNumber)
                .withFailMessage("Expected order number to start with '%s', but was: %s", expectedPrefix, actualOrderNumber)
                .startsWith(expectedPrefix);
    }

    public ViewOrderVerification orderTimestamp(Instant expectedTimestamp) {
        var actualTimestamp = getResponse().getOrderTimestamp();
        assertThat(actualTimestamp)
                .withFailMessage("Expected order timestamp to be %s, but was %s", expectedTimestamp, actualTimestamp)
                .isEqualTo(expectedTimestamp);
        return this;
    }

    public ViewOrderVerification orderTimestamp(String expectedTimestamp) {
        return orderTimestamp(Converter.toInstant(expectedTimestamp));
    }

    public ViewOrderVerification orderTimestampIsNotNull() {
        var actualTimestamp = getResponse().getOrderTimestamp();
        assertThat(actualTimestamp)
                .withFailMessage("Expected order timestamp to be set, but was null")
                .isNotNull();
        return this;
    }

}
