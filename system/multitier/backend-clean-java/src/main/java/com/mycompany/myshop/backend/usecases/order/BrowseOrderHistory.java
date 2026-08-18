package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.usecases.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.queries.OrderListItem;
import com.mycompany.myshop.backend.usecases.queries.OrderQuery;

/**
 * A pure query. The optional order-number filter goes to the port instead of branching here, so the
 * {@code LIKE} stays in SQL and this use case has nothing left to decide.
 */
public class BrowseOrderHistory implements UseCase<BrowseOrderHistoryRequest, BrowseOrderHistoryResponse> {

    private final OrderQuery orderQuery;

    public BrowseOrderHistory(OrderQuery orderQuery) {
        this.orderQuery = orderQuery;
    }

    @Override
    public Result<BrowseOrderHistoryResponse, UseCaseError> execute(BrowseOrderHistoryRequest request) {
        var items = orderQuery.listOrders(request.orderNumberFilter()).stream()
                .map(BrowseOrderHistory::toItem)
                .toList();

        var response = new BrowseOrderHistoryResponse();
        response.setOrders(items);
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
