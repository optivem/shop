package com.optivem.shop.dsl.driver.adapter.shop.ui.client.pages;

import com.optivem.shop.dsl.common.Converter;
import com.optivem.shop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.shop.dsl.driver.adapter.shared.client.playwright.PageClient;

import java.math.BigDecimal;
import java.time.Instant;

public class OrderDetailsPage extends BasePage {
    private static final String ORDER_NUMBER_OUTPUT_SELECTOR = "[aria-label='Display Order Number']";
    private static final String ORDER_TIMESTAMP_OUTPUT_SELECTOR = "[aria-label='Display Order Timestamp']";
    private static final String SKU_OUTPUT_SELECTOR = "[aria-label='Display SKU']";
    private static final String COUNTRY_OUTPUT_SELECTOR = "[aria-label='Display Country']";
    private static final String QUANTITY_OUTPUT_SELECTOR = "[aria-label='Display Quantity']";
    private static final String UNIT_PRICE_OUTPUT_SELECTOR = "[aria-label='Display Unit Price']";
    private static final String BASE_PRICE_OUTPUT_SELECTOR = "[aria-label='Display Base Price']";
    private static final String SUBTOTAL_PRICE_OUTPUT_SELECTOR = "[aria-label='Display Subtotal Price']";
    private static final String DISCOUNT_RATE_OUTPUT_SELECTOR = "[aria-label='Display Discount Rate']";
    private static final String DISCOUNT_AMOUNT_OUTPUT_SELECTOR = "[aria-label='Display Discount Amount']";
    private static final String TAX_RATE_OUTPUT_SELECTOR = "[aria-label='Display Tax Rate']";
    private static final String TAX_AMOUNT_OUTPUT_SELECTOR = "[aria-label='Display Tax Amount']";
    private static final String TOTAL_PRICE_OUTPUT_SELECTOR = "[aria-label='Display Total Price']";
    private static final String STATUS_OUTPUT_SELECTOR = "[aria-label='Display Status']";
    private static final String APPLIED_COUPON_OUTPUT_SELECTOR = "[aria-label='Display Applied Coupon']";
    private static final String CANCEL_ORDER_SELECTOR = "[aria-label='Cancel Order']";
    private static final String TEXT_NONE = "None";
    private static final String DOLLAR_SYMBOL = "$";
    private static final String PERCENT_SYMBOL = "%";

    public OrderDetailsPage(PageClient pageClient) {
        super(pageClient);
    }

    public boolean isLoadedSuccessfully() {
        return pageClient.isVisible(ORDER_NUMBER_OUTPUT_SELECTOR);
    }

    public String getOrderNumber() {
        return pageClient.readTextContent(ORDER_NUMBER_OUTPUT_SELECTOR);
    }

    public Instant getOrderTimestamp() {
        var textContent = pageClient.readTextContent(ORDER_TIMESTAMP_OUTPUT_SELECTOR);
        return Converter.toInstant(textContent);
    }

    public String getSku() {
        return pageClient.readTextContent(SKU_OUTPUT_SELECTOR);
    }

    public String getCountry() {
        return pageClient.readTextContent(COUNTRY_OUTPUT_SELECTOR);
    }

    public int getQuantity() {
        var textContent = pageClient.readTextContent(QUANTITY_OUTPUT_SELECTOR);
        return Integer.parseInt(textContent);
    }

    public BigDecimal getUnitPrice() {
        return readTextMoney(UNIT_PRICE_OUTPUT_SELECTOR);
    }

    public BigDecimal getBasePrice() {
        return readTextMoney(BASE_PRICE_OUTPUT_SELECTOR);
    }

    public BigDecimal getDiscountRate() {
        return readTextPercentage(DISCOUNT_RATE_OUTPUT_SELECTOR);
    }

    public BigDecimal getDiscountAmount() {
        return readTextMoney(DISCOUNT_AMOUNT_OUTPUT_SELECTOR);
    }

    public BigDecimal getSubtotalPrice() {
        return readTextMoney(SUBTOTAL_PRICE_OUTPUT_SELECTOR);
    }

    public BigDecimal getTaxRate() {
        return readTextPercentage(TAX_RATE_OUTPUT_SELECTOR);
    }

    public BigDecimal getTaxAmount() {
        return readTextMoney(TAX_AMOUNT_OUTPUT_SELECTOR);
    }

    public BigDecimal getTotalPrice() {
        return readTextMoney(TOTAL_PRICE_OUTPUT_SELECTOR);
    }

    public OrderStatus getStatus() {
        var status = pageClient.readTextContent(STATUS_OUTPUT_SELECTOR);
        return OrderStatus.valueOf(status);
    }

    public String getAppliedCoupon() {
        var coupon = pageClient.readTextContent(APPLIED_COUPON_OUTPUT_SELECTOR);
        return TEXT_NONE.equals(coupon) ? null : coupon;
    }

    public void clickCancelOrder() {
        pageClient.click(CANCEL_ORDER_SELECTOR);
    }

    public boolean isCancelButtonHidden() {
        return pageClient.isHidden(CANCEL_ORDER_SELECTOR);
    }

    private BigDecimal readTextMoney(String selector) {
        var textContent = pageClient.readTextContent(selector);
        var cleaned = textContent.replace(DOLLAR_SYMBOL, "").trim();
        return new BigDecimal(cleaned);
    }

    private BigDecimal readTextPercentage(String selector) {
        var textContent = pageClient.readTextContent(selector);
        var cleaned = textContent.replace(PERCENT_SYMBOL, "").trim();
        var value = new BigDecimal(cleaned);
        return value.divide(BigDecimal.valueOf(100));
    }
}
