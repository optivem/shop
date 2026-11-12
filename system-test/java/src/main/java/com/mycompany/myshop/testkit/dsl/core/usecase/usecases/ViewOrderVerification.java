package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.ShippingAddress;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewOrderResponse;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.common.Converter;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies a viewed order. Money and rates are written as {@code String} literals
 * ({@code "20.00"}, {@code "0.10"}); there is no numeric ({@code double}) form, because a
 * floating-point literal loses the author's decimal before the DSL sees it. The {@code BigDecimal}
 * overloads are the internal canonical type the {@code String} forms parse into, and comparison uses
 * {@code isEqualByComparingTo}, so scale does not matter ({@code "0.1"} equals {@code "0.10"}).
 */
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

    public ViewOrderVerification orderNumberIsNot(String orderNumberResultAlias) {
        var otherOrderNumber = getContext().getResultValue(orderNumberResultAlias);
        var actualOrderNumber = getResponse().getOrderNumber();
        assertThat(actualOrderNumber)
                .withFailMessage("Expected order number to differ from '%s', but it was the same", otherOrderNumber)
                .isNotEqualTo(otherOrderNumber);
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

    public ViewOrderVerification country(String expectedCountryAlias) {
        var expectedCountry = getContext().getParamValueOrLiteral(expectedCountryAlias);
        var actualCountry = getResponse().getCountry();
        assertThat(actualCountry)
                .withFailMessage("Expected country to be '%s', but was '%s'", expectedCountry, actualCountry)
                .isEqualTo(expectedCountry);
        return this;
    }

    public ViewOrderVerification unitPrice(BigDecimal expectedUnitPrice) {
        var actualPrice = getResponse().getUnitPrice();
        assertThat(actualPrice)
                .withFailMessage("Expected unit price to be %s, but was %s", expectedUnitPrice, actualPrice)
                .isEqualByComparingTo(expectedUnitPrice);
        return this;
    }

    public ViewOrderVerification unitPrice(String expectedUnitPrice) {
        return unitPrice(Converter.toBigDecimal(expectedUnitPrice));
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

    public ViewOrderVerification subtotalPrice(String expectedSubtotalPrice) {
        return subtotalPrice(Converter.toBigDecimal(expectedSubtotalPrice));
    }

    public ViewOrderVerification basePrice(BigDecimal expectedBasePrice) {
        var actualBasePrice = getResponse().getBasePrice();
        assertThat(actualBasePrice)
                .withFailMessage("Expected base price to be %s, but was %s", expectedBasePrice, actualBasePrice)
                .isEqualByComparingTo(expectedBasePrice);
        return this;
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

    public ViewOrderVerification discountRate(String expectedDiscountRate) {
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

    public ViewOrderVerification noAppliedCoupon() {
        assertThat(getResponse().getAppliedCouponCode())
                .as("Applied coupon code (none)")
                .isNull();
        return this;
    }

    /** UI only: whether the order details offer Cancel Order. */
    public ViewOrderVerification cancelOffered(boolean expected) {
        assertThat(getResponse().getCancelOffered())
                .as("Cancel Order offered (UI only)")
                .isEqualTo(expected);
        return this;
    }

    /** UI only: whether the order details offer Deliver Order. */
    public ViewOrderVerification deliverOffered(boolean expected) {
        assertThat(getResponse().getDeliverOffered())
                .as("Deliver Order offered (UI only)")
                .isEqualTo(expected);
        return this;
    }

    /** The order's shipping address; the country is an alias, resolved like {@link #country(String)}. */
    public ViewOrderVerification shippingAddress(String firstName, String lastName, String street, String postalCode,
                                                 String city, String countryAlias) {
        var expected = ShippingAddress.builder()
                .firstName(firstName)
                .lastName(lastName)
                .street(street)
                .postalCode(postalCode)
                .city(city)
                .country(getContext().getParamValueOrLiteral(countryAlias))
                .build();
        var actual = getResponse().getShippingAddress();
        assertThat(actual)
                .withFailMessage("Expected shipping address to be %s, but was %s", expected, actual)
                .isEqualTo(expected);
        return this;
    }

    /** The order's country as a code, such as "DE", not an alias: checkout offers only real shipping countries. */
    public ViewOrderVerification countryCode(String expectedCountry) {
        var actualCountry = getResponse().getCountry();
        assertThat(actualCountry)
                .withFailMessage("Expected country to be '%s', but was '%s'", expectedCountry, actualCountry)
                .isEqualTo(expectedCountry);
        return this;
    }

    /** The order's shipping address, with its country as a code, not an alias. */
    public ViewOrderVerification shippingAddressWithCountryCode(String firstName, String lastName, String street,
                                                                String postalCode, String city, String country) {
        var expected = ShippingAddress.builder()
                .firstName(firstName)
                .lastName(lastName)
                .street(street)
                .postalCode(postalCode)
                .city(city)
                .country(country)
                .build();
        var actual = getResponse().getShippingAddress();
        assertThat(actual)
                .withFailMessage("Expected shipping address to be %s, but was %s", expected, actual)
                .isEqualTo(expected);
        return this;
    }

    /** The SKU the order is for, as the system returned it. */
    public String orderedSku() {
        return getResponse().getSku();
    }

    public ViewOrderVerification noShippingAddress() {
        var actual = getResponse().getShippingAddress();
        assertThat(actual)
                .withFailMessage("Expected the order to have no shipping address, but it was %s", actual)
                .isNull();
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
