package com.mycompany.myshop.backend.infrastructure.persistence.adapters;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.infrastructure.persistence.mappers.OrderMapper;
import com.mycompany.myshop.backend.infrastructure.persistence.repositories.OrderJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Implements the domain's {@link OrderRepository} port with Spring Data JPA. The interface is owned
 * by the inside; this implementation is owned by the outside.
 */
@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order save(Order order) {
        var saved = jpaRepository.save(OrderMapper.toEntity(order));
        order.setId(saved.getId());
        return OrderMapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return jpaRepository.findByOrderNumber(orderNumber).map(OrderMapper::toDomain);
    }

    @Override
    public List<Order> findAllByOrderByOrderTimestampDesc() {
        return jpaRepository.findAllByOrderByOrderTimestampDesc().stream()
                .map(OrderMapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc(String orderNumber) {
        return jpaRepository.findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc(orderNumber).stream()
                .map(OrderMapper::toDomain)
                .toList();
    }
}
