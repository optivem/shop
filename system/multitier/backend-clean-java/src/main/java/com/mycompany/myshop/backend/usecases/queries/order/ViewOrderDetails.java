package com.mycompany.myshop.backend.usecases.queries.order;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.queries.order.ports.OrderReader;

// A pure query: every field of the response is a column in orders, so the domain model is
// not built at all. The error contract does not move -- an absent row is still
// UseCaseError.NotFound.
public class ViewOrderDetails implements UseCase<ViewOrderDetailsRequest, ViewOrderDetailsResponse> {

    private static final String ORDER_ENTITY = "Order";

    private final OrderReader orderReader;

    public ViewOrderDetails(OrderReader orderReader) {
        this.orderReader = orderReader;
    }

    @Override
    public Result<ViewOrderDetailsResponse, UseCaseError> execute(ViewOrderDetailsRequest request) {
        var orderNumber = request.orderNumber();
        var found = orderReader.findOrderDetail(orderNumber);
        if (found.isEmpty()) {
            return Result.err(new UseCaseError.NotFound(ORDER_ENTITY, orderNumber));
        }

        return Result.ok(found.get());
    }
}
