package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.exceptions.NotExistValidationException;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;

/**
 * Marks an order as delivered. Whether this order may be delivered is the order's own decision.
 */
public class DeliverOrder {

    private final OrderRepository orderRepository;

    public DeliverOrder(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void execute(String orderNumber) {
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            throw new ValidationException("Order number must not be empty");
        }

        var order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotExistValidationException("Order " + orderNumber + " does not exist."));

        order.deliver();
        orderRepository.save(order);
    }
}
