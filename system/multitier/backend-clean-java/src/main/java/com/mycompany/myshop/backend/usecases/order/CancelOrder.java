package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.services.YearEndBlackoutPolicy;
import com.mycompany.myshop.backend.domain.values.OrderNumber;
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
        // Before the lookup, as it always was: during the blackout window the answer is the same
        // whether or not the order exists.
        YearEndBlackoutPolicy.requireCancellationAllowed(clockGateway.getCurrentTime());

        // A missing order number is reported the same way as an unknown one: this use case has never
        // distinguished them, so it discards the violation rather than reporting it. This is the
        // disagreement with DeliverOrder that keeps OrderNumber.parse returning a Result instead of
        // throwing like the rest -- one refusal, two callers who answer it differently.
        var found = OrderNumber.parse(request.orderNumber())
                .toOptional()
                .flatMap(orderRepository::findByOrderNumber);
        if (found.isEmpty()) {
            return Result.err(new UseCaseError.NotFound(ORDER_ENTITY, request.orderNumber()));
        }
        var order = found.get();

        order.cancel();

        orderRepository.update(order);
        return Result.ok();
    }
}
