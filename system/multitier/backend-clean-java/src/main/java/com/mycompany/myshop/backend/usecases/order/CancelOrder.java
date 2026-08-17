package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.exceptions.NotExistValidationException;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.policies.YearEndBlackoutPolicy;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;

/**
 * Cancels an order. Whether cancellation is open is the calendar policy's decision and whether this
 * order may still be cancelled is the order's own.
 */
public class CancelOrder {

    private final OrderRepository orderRepository;
    private final ClockGateway clockGateway;

    public CancelOrder(OrderRepository orderRepository, ClockGateway clockGateway) {
        this.orderRepository = orderRepository;
        this.clockGateway = clockGateway;
    }

    public void execute(String orderNumber) {
        YearEndBlackoutPolicy.requireCancellationAllowed(clockGateway.getCurrentTime());

        var order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotExistValidationException("Order " + orderNumber + " does not exist."));

        order.cancel();
        orderRepository.save(order);
    }
}
