package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.queries.OrderCursor;
import com.mycompany.myshop.backend.usecases.queries.OrderListItem;
import com.mycompany.myshop.backend.usecases.queries.OrderQuery;
import com.mycompany.myshop.backend.usecases.queries.PageSpec;

// A pure query. The optional order-number filter and the page both go to the port instead of
// branching here, so the LIKE and the LIMIT stay in SQL and this use case has
// nothing left to decide but how big a page it is willing to ask for.
//
// That bound is the use case's to enforce and nobody else's: the adapter would honour any number
// it is handed, and the controller is the layer that has just been told a number by a stranger.
public class BrowseOrderHistory implements UseCase<BrowseOrderHistoryRequest, BrowseOrderHistoryResponse> {

    private static final String FIELD_SIZE = "size";

    private final OrderQuery orderQuery;

    public BrowseOrderHistory(OrderQuery orderQuery) {
        this.orderQuery = orderQuery;
    }

    @Override
    public Result<BrowseOrderHistoryResponse, UseCaseError> execute(BrowseOrderHistoryRequest request) {
        if (!PageSpec.isValidSize(request.size())) {
            return Result.err(new UseCaseError.Invalid(FIELD_SIZE,
                    "Page size must be between 1 and " + PageSpec.MAX_SIZE));
        }

        var page = orderQuery.listOrders(
                request.orderNumberFilter(),
                new PageSpec<>(PageSpec.sizeOrDefault(request.size()), request.cursor()));

        var response = new BrowseOrderHistoryResponse();
        response.setOrders(page.items().stream().map(BrowseOrderHistory::toItem).toList());
        response.setHasMore(page.hasMore());
        // No more rows means no next page, so there is nothing to resume from. Handing back a cursor
        // anyway would invite a client to ask for a page that is known to be empty.
        response.setNextCursor(page.hasMore()
                ? page.last().map(OrderCursor::after).orElse(null)
                : null);
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
