package com.mycompany.myshop.backend.infrastructure.persistence.adapters;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.values.OrderNumber;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.domain.values.Sku;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.infrastructure.persistence.mappers.OrderMapper;
import com.mycompany.myshop.backend.infrastructure.persistence.repositories.OrderJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    // A mapped entity carries no id, so Spring Data sees a new instance and persists it: one INSERT,
    // no SELECT. The old `save` looked the order up first to find out whether it already had a row --
    // for an order number PlaceOrder had just generated from a UUID, so the answer was always no.
    @Override
    public void add(Order order) {
        jpaRepository.save(OrderMapper.toEntity(order));
    }

    @Transactional
    @Override
    public void update(Order order) {
        jpaRepository.updateStatus(order.getOrderNumber().value(), order.getStatus());
    }

    @Override
    public Optional<Order> findByOrderNumber(OrderNumber orderNumber) {
        return jpaRepository.findByOrderNumber(orderNumber.value()).map(OrderMapper::toDomain);
    }

    @Transactional
    @Override
    public int cancelOutstandingForSku(Sku sku) {
        return jpaRepository.cancelOutstandingForSku(sku.value(), OrderStatus.PLACED, OrderStatus.CANCELLED);
    }

    @Transactional
    @Override
    public int deliverPlacedOlderThan(Instant cutoff) {
        return jpaRepository.deliverPlacedOlderThan(cutoff, OrderStatus.PLACED, OrderStatus.DELIVERED);
    }
}
