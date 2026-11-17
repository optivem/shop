package com.mycompany.myshop.backend.core.outbox;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The event a placed order publishes through the outbox. It carries what a downstream consumer needs
 * without reading the orders table: which order, what was bought, what it cost and when.
 * {@code totalPrice} is the stored (rounded) total; the app's mapper writes it as a string with two
 * decimals.
 */
public record OrderPlaced(String orderNumber, String sku, int quantity, BigDecimal totalPrice, Instant placedAt) {

    public static final String EVENT_TYPE = "OrderPlaced";
    public static final String AGGREGATE_TYPE = "Order";
}
