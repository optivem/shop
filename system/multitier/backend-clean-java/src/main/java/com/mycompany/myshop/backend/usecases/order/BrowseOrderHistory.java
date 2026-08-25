package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.queries.OrderListItem;
import com.mycompany.myshop.backend.usecases.queries.OrderQuery;
import com.mycompany.myshop.backend.usecases.queries.PageSpec;

// A pure query. The optional order-number filter and the page both go to the port instead of
// branching here, so the LIKE and the OFFSET stay in SQL and this use case has
// nothing left to decide but which pages it is willing to ask for.
//
// Those bounds are the use case's to enforce and nobody else's: the adapter would honour any
// numbers it is handed, and the controller is the layer that has just been told two numbers by a
// stranger. A page below the first is rejected; a page past the last is not, because "page 900 of
// 26" is an empty list rather than a mistake, and a client walking off the end should see the end.
public class BrowseOrderHistory implements UseCase<BrowseOrderHistoryRequest, BrowseOrderHistoryResponse> {

    private static final String FIELD_PAGE = "page";
    private static final String FIELD_SIZE = "size";

    private final OrderQuery orderQuery;

    public BrowseOrderHistory(OrderQuery orderQuery) {
        this.orderQuery = orderQuery;
    }

    @Override
    public Result<BrowseOrderHistoryResponse, UseCaseError> execute(BrowseOrderHistoryRequest request) {
        if (!PageSpec.isValidPage(request.page())) {
            return Result.err(new UseCaseError.Invalid(FIELD_PAGE,
                    "Page must be " + PageSpec.FIRST_PAGE + " or greater"));
        }
        if (!PageSpec.isValidSize(request.size())) {
            return Result.err(new UseCaseError.Invalid(FIELD_SIZE,
                    "Page size must be between 1 and " + PageSpec.MAX_SIZE));
        }

        var page = orderQuery.listOrders(
                request.orderNumberFilter(),
                new PageSpec(PageSpec.pageOrFirst(request.page()), PageSpec.sizeOrDefault(request.size())));

        var response = new BrowseOrderHistoryResponse();
        response.setOrders(page.items().stream().map(BrowseOrderHistory::toItem).toList());
        response.setPage(page.page());
        response.setSize(page.size());
        response.setTotalElements(page.totalElements());
        response.setTotalPages(page.totalPages());
        return Result.ok(response);
    }

    private static BrowseOrderHistoryResponse.BrowseOrderHistoryItemResponse toItem(OrderListItem order) {
        var item = new BrowseOrderHistoryResponse.BrowseOrderHistoryItemResponse();
        item.setOrderNumber(order.orderNumber());
        item.setOrderTimestamp(order.orderTimestamp());
        item.setSku(order.sku());
        item.setCountry(order.country());
        item.setQuantity(order.quantity());
        item.setTotalPrice(order.totalPrice());
        item.setStatus(order.status());
        item.setAppliedCouponCode(order.appliedCouponCode());
        return item;
    }
}
