package com.mycompany.myshop.backend.usecases.queries.order;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.queries.order.ports.OrderReader;

// The choices of the admin customer filter: everyone who has placed an order. A pure query with no
// bounds to enforce -- the list is one row per customer rather than per order, so it is unpaged by
// design, and the distinct and the sort both stay in SQL.
//
// The customer is read from the stored owner_name column. This variant has no authentication, so
// its own PlaceOrder never fills that column; an order placed through a backend that does is listed
// all the same, because the schema is shared.
public class BrowseCustomers implements UseCase<BrowseCustomersRequest, BrowseCustomersResponse> {

    private final OrderReader orderReader;

    public BrowseCustomers(OrderReader orderReader) {
        this.orderReader = orderReader;
    }

    @Override
    public Result<BrowseCustomersResponse, UseCaseError> execute(BrowseCustomersRequest request) {
        return Result.ok(new BrowseCustomersResponse(orderReader.listCustomers()));
    }
}
