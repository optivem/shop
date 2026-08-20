package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.services.YearEndBlackoutPolicy;
import com.mycompany.myshop.backend.domain.values.OrderNumber;
import com.mycompany.myshop.backend.usecases.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;

public class CancelOrder implements UseCase<CancelOrderRequest, Void> {

    private static final String ORDER_ENTITY = "Order";

    private final OrderRepository orderRepository;
    private final ClockGateway clockGateway;

    public CancelOrder(OrderRepository orderRepository, ClockGateway clockGateway) {
        this.orderRepository = orderRepository;
        this.clockGateway = clockGateway;
    }

    @Override
    public Result<Void, UseCaseError> execute(CancelOrderRequest request) {
        try {
            // Before the lookup, as it always was: during the blackout window the answer is the same
            // whether or not the order exists.
            YearEndBlackoutPolicy.requireCancellationAllowed(clockGateway.getCurrentTime());
        } catch (ValidationException e) {
            return Result.err(UseCaseError.from(e));
        }

        // A missing order number is reported the same way as an unknown one: this use case has
        // never distinguished them.
        var orderNumber = OrderNumber.requested(request.orderNumber());
        var order = orderNumber.flatMap(orderRepository::findByOrderNumber);
        if (order.isEmpty()) {
            return Result.err(new UseCaseError.NotFound(ORDER_ENTITY, request.orderNumber()));
        }

        try {
            order.get().cancel();
        } catch (ValidationException e) {
            return Result.err(UseCaseError.from(e));
        }

        orderRepository.save(order.get());
        return Result.ok(null);
    }
}
