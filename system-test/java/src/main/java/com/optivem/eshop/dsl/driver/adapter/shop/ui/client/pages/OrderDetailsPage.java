package com.optivem.eshop.dsl.driver.adapter.shop.ui.client.pages;

import com.optivem.eshop.dsl.common.Converter;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.eshop.dsl.driver.adapter.shared.client.playwright.PageClient;

import java.math.BigDecimal;
import java.time.Instant;


public class OrderDetailsPage extends BasePage {
    private static final String ORDER_NUMBER_OUTPUT_SELECTOR = "[aria-label='Display Order Number']";
    private static final String ORDER_TIMESTAMP_OUTPUT_SELECTOR = "[aria-label='Display Order Timestamp']";
    private static final String SKU_OUTPUT_SELECTOR = "[aria-label='Display SKU']";
    private static final String QUANTITY_OUTPUT_SELECTOR = "[aria-label='Display Quantity']";
    private static final String UNIT_PRICE_OUTPUT_SELECTOR = "[aria-label='Display Unit Price']";
    private static final String TOTAL_PRICE_OUTPUT_SELECTOR = "[aria-label='Display Total Price']";
    private static final String STATUS_OUTPUT_SELECTOR = "[aria-label='Display Status']";
    private static final String DOLLAR_SYMBOL = "$";

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

    public int getQuantity() {
        var textContent = pageClient.readTextContent(QUANTITY_OUTPUT_SELECTOR);
        return Integer.parseInt(textContent);
    }

    public BigDecimal getUnitPrice() {
        return readTextMoney(UNIT_PRICE_OUTPUT_SELECTOR);
    }

    public BigDecimal getTotalPrice() {
        return readTextMoney(TOTAL_PRICE_OUTPUT_SELECTOR);
    }

    public OrderStatus getStatus() {
        var status = pageClient.readTextContent(STATUS_OUTPUT_SELECTOR);
        return OrderStatus.valueOf(status);
    }

    private BigDecimal readTextMoney(String selector) {
        var textContent = pageClient.readTextContent(selector);
        var cleaned = textContent.replace(DOLLAR_SYMBOL, "").trim();
        return new BigDecimal(cleaned);
    }

}




