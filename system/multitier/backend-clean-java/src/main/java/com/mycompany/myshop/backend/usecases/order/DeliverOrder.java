package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.usecases.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;

public class DeliverOrder implements UseCase<DeliverOrderRequest, Void> {

    private static final String ORDER_ENTITY = "Order";

    private final OrderRepository orderRepository;

    public DeliverOrder(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Result<Void, UseCaseError> execute(DeliverOrderRequest request) {
        var orderNumber = request.orderNumber();
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            return Result.err(new UseCaseError.Invalid(null, "Order number must not be empty"));
        }

        var order = orderRepository.findByOrderNumber(orderNumber);
        if (order.isEmpty()) {
            return Result.err(new UseCaseError.NotFound(ORDER_ENTITY, orderNumber));
        }

        try {
            order.get().deliver();
        } catch (ValidationException e) {
            return Result.err(UseCaseError.from(e));
        }

        orderRepository.save(order.get());
        return Result.ok(null);
    }
}
