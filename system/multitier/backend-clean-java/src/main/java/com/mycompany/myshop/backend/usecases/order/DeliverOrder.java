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

    // Two things are still returned here, and neither is a domain refusal. `parse` returns because
    // its two callers genuinely disagree about what a malformed order number means -- this one
    // reports it as malformed, CancelOrder reports it as not-found -- which is the test that earns a
    // Result. `findByOrderNumber` returns an Optional because a missing row is an absent value, not
    // a rule saying no.
    //
    // The refusal, `deliver`, is the one that throws, and this method is where that pays: the line
    // that delivers the order is the line that delivers the order.
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

        order.deliver();

        orderRepository.update(order);
        return Result.ok();
    }
}
