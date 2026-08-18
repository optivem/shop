package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.usecases.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;

import java.util.List;

/**
 * Lists orders newest-first, optionally narrowed to those whose order number contains a filter.
 */
public class BrowseOrderHistory implements UseCase<BrowseOrderHistoryRequest, BrowseOrderHistoryResponse> {

    private final OrderRepository orderRepository;

    public BrowseOrderHistory(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Result<BrowseOrderHistoryResponse, UseCaseError> execute(BrowseOrderHistoryRequest request) {
        var orderNumberFilter = request.orderNumberFilter();

        List<Order> orders;
        if (orderNumberFilter == null || orderNumberFilter.trim().isEmpty()) {
            orders = orderRepository.findAllByOrderByOrderTimestampDesc();
        } else {
            orders = orderRepository.findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc(orderNumberFilter.trim());
        }

        var items = orders.stream()
                .map(BrowseOrderHistory::toItem)
                .toList();

        var response = new BrowseOrderHistoryResponse();
        response.setOrders(items);
        return Result.ok(response);
    }

    private static BrowseOrderHistoryResponse.BrowseOrderHistoryItemResponse toItem(Order order) {
        var item = new BrowseOrderHistoryResponse.BrowseOrderHistoryItemResponse();
        item.setOrderNumber(order.getOrderNumber());
        item.setOrderTimestamp(order.getOrderTimestamp());
        item.setSku(order.getSku());
        item.setCountry(order.getCountry().value());
        item.setQuantity(order.getQuantity());
        item.setTotalPrice(order.getTotalPrice().amount());
        item.setStatus(order.getStatus());
        item.setAppliedCouponCode(CouponCode.valueOrNull(order.getAppliedCouponCode()));
        return item;
    }
}
