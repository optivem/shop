package com.mycompany.myshop.backend.domain.repositories;

import com.mycompany.myshop.backend.domain.entities.Order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findAllByOrderByOrderTimestampDesc();

    List<Order> findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc(String orderNumber);

    int cancelOutstandingForSku(String sku);

    int deliverPlacedOlderThan(Instant cutoff);
}
