package com.mycompany.myshop.backend.domain.repositories;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.values.OrderNumber;
import com.mycompany.myshop.backend.domain.values.Sku;

import java.time.Instant;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findByOrderNumber(OrderNumber orderNumber);

    int cancelOutstandingForSku(Sku sku);

    int deliverPlacedOlderThan(Instant cutoff);
}
