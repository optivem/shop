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

    @Override
    public Order save(Order order) {
        var entity = OrderMapper.toEntity(order);
        jpaRepository.findByOrderNumber(order.getOrderNumber().value())
                .ifPresent(existing -> entity.setId(existing.getId()));
        return OrderMapper.toDomain(jpaRepository.save(entity));
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
