package com.mycompany.myshop.backend.domain.repositories;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.values.OrderNumber;
import com.mycompany.myshop.backend.domain.values.Sku;

import java.time.Instant;
import java.util.Optional;

public interface OrderRepository {

    // `add` and `update` rather than one `save`, because no caller has ever been unsure which it
    // meant: PlaceOrder has just minted an order number that cannot already exist, and CancelOrder
    // and DeliverOrder are holding a row they just read. A single `save` threw that knowledge away
    // and paid the database to work it out again -- one wasted SELECT per write, on the hot path.
    void add(Order order);

    void update(Order order);

    Optional<Order> findByOrderNumber(OrderNumber orderNumber);

    int cancelOutstandingForSku(Sku sku);

    int deliverPlacedOlderThan(Instant cutoff);
}
