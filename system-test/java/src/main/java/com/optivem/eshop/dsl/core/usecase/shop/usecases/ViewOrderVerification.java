package com.optivem.eshop.dsl.core.usecase.shop.usecases;

import com.optivem.eshop.dsl.driver.port.shop.dtos.ViewOrderResponse;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.eshop.dsl.core.shared.ResponseVerification;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;
import com.optivem.eshop.dsl.common.Converter;

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
                .isEqualTo(expectedTotalPrice);
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



