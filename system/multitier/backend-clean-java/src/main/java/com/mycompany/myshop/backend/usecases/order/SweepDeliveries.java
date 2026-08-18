package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.usecases.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;

import java.time.temporal.ChronoUnit;

public class SweepDeliveries implements UseCase<SweepDeliveriesRequest, SweepDeliveriesResponse> {

    static final int DEFAULT_OLDER_THAN_DAYS = 7;

    private static final String FIELD_OLDER_THAN_DAYS = "olderThanDays";

    private final OrderRepository orderRepository;
    private final ClockGateway clockGateway;

    public SweepDeliveries(OrderRepository orderRepository, ClockGateway clockGateway) {
        this.orderRepository = orderRepository;
        this.clockGateway = clockGateway;
    }

    @Override
    public Result<SweepDeliveriesResponse, UseCaseError> execute(SweepDeliveriesRequest request) {
        var olderThanDays = request.olderThanDays() == null
                ? DEFAULT_OLDER_THAN_DAYS
                : request.olderThanDays();
        if (olderThanDays < 1) {
            return Result.err(new UseCaseError.Invalid(FIELD_OLDER_THAN_DAYS,
                    "olderThanDays must be at least 1"));
        }

        var cutoff = clockGateway.getCurrentTime().minus(olderThanDays, ChronoUnit.DAYS);

        var response = new SweepDeliveriesResponse();
        response.setCutoff(cutoff);
        response.setDeliveredCount(orderRepository.deliverPlacedOlderThan(cutoff));
        return Result.ok(response);
    }
}
