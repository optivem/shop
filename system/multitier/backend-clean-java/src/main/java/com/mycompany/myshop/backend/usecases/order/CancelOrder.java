package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.domain.entities.CancelOutcome;
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
        var allowed = YearEndBlackoutPolicy.cancellationAllowed(clockGateway.getCurrentTime());
        if (!allowed.isOk()) {
            return Result.err(UseCaseError.from(allowed.error()));
        }

        // A missing order number is reported the same way as an unknown one: this use case has never
        // distinguished them, so it discards the violation rather than reporting it.
        var order = OrderNumber.requested(request.orderNumber())
                .toOptional()
                .flatMap(orderRepository::findByOrderNumber);
        if (order.isEmpty()) {
            return Result.err(new UseCaseError.NotFound(ORDER_ENTITY, request.orderNumber()));
        }

        // The compiler will not let a new CancelOutcome be added without this switch being revisited.
        return switch (order.get().cancel()) {
            case CancelOutcome.Cancelled ignored -> {
                orderRepository.update(order.get());
                yield Result.ok(null);
            }
            case CancelOutcome.AlreadyCancelled alreadyCancelled ->
                    Result.err(UseCaseError.from(alreadyCancelled.violation()));
            case CancelOutcome.NotCancellable notCancellable ->
                    Result.err(UseCaseError.from(notCancellable.violation()));
        };
    }
}
