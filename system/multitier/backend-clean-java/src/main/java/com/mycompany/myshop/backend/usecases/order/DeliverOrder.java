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
        var parsed = OrderNumber.parse(request.orderNumber());
        if (!parsed.isOk()) {
            return Result.err(UseCaseError.from(parsed.error()));
        }
        var orderNumber = parsed.value();

        var found = orderRepository.findByOrderNumber(orderNumber);
        if (found.isEmpty()) {
            return Result.err(new UseCaseError.NotFound(ORDER_ENTITY, orderNumber.value()));
        }
        var order = found.get();

        var delivered = order.deliver();
        if (!delivered.isOk()) {
            return Result.err(UseCaseError.from(delivered.error()));
        }

        orderRepository.update(order);
        return Result.ok();
    }
}
