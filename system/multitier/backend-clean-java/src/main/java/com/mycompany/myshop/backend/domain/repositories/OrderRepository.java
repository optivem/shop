package com.mycompany.myshop.backend.domain.repositories;

import com.mycompany.myshop.backend.domain.entities.Order;

import java.util.List;
import java.util.Optional;

/**
 * The port to order storage. A plain interface — no Spring Data, no JPA. Implemented by
 * {@code infrastructure.persistence.adapters.OrderRepositoryAdapter}.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findAllByOrderByOrderTimestampDesc();

    List<Order> findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc(String orderNumber);
}
