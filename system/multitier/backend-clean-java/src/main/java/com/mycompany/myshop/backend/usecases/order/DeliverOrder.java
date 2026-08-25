package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.OrderNumber;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;

public class DeliverOrder implements UseCase<DeliverOrderRequest, Void> {

    private static final String ORDER_ENTITY = "Order";

    private final OrderRepository orderRepository;

    public DeliverOrder(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // No try, no catch, and nothing a future edit can forget: every way this can be refused arrives
    // as a value that the compiler will not let the next line ignore.
    @Override
    public Result<Void, UseCaseError> execute(DeliverOrderRequest request) {
        var orderNumber = OrderNumber.requested(request.orderNumber());
        if (!orderNumber.isOk()) {
            return Result.err(UseCaseError.from(orderNumber.error()));
        }

        var order = orderRepository.findByOrderNumber(orderNumber.value());
        if (order.isEmpty()) {
            return Result.err(new UseCaseError.NotFound(ORDER_ENTITY, orderNumber.value().value()));
        }

        var delivered = order.get().deliver();
        if (!delivered.isOk()) {
            return Result.err(UseCaseError.from(delivered.error()));
        }

        orderRepository.update(order.get());
        return Result.ok(null);
    }
}
