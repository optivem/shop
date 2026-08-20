package com.mycompany.myshop.backend.usecases.queries;

import java.time.Instant;

// Where an order list left off: the sort key of the last row returned.
//
// The tiebreaker is the order number rather than the surrogate id. Textbook keyset
// pagination reaches for the primary key, and this codebase has no primary key to reach for -- a
// domain Order carries no Long id, the adapter resolves it on the way to the table.
// The constraint forces the honest version: a cursor is handed to a client, so its key has to be a
// column that is unique, stable, and already public. That is exactly what order_number is,
// and a surrogate id is none of the three.
//
// Both fields together, never either alone. order_timestamp is not unique, so a cursor
// holding only the timestamp would skip rows or repeat them at every page boundary where two orders
// share an instant.
public record OrderCursor(Instant orderTimestamp, String orderNumber) {

    public static OrderCursor after(OrderListItem item) {
        return new OrderCursor(item.orderTimestamp(), item.orderNumber());
    }
}
