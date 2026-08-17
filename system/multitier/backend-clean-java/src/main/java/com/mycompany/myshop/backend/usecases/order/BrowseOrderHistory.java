package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.usecases.dtos.BrowseOrderHistoryResponse;

import java.util.List;

/**
 * Lists orders newest-first, optionally narrowed to those whose order number contains a filter.
 */
public class BrowseOrderHistory {

    private final OrderRepository orderRepository;

    public BrowseOrderHistory(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public BrowseOrderHistoryResponse execute(String orderNumberFilter) {
        List<Order> orders;
        if (orderNumberFilter == null || orderNumberFilter.trim().isEmpty()) {
            orders = orderRepository.findAllByOrderByOrderTimestampDesc();
        } else {
            orders = orderRepository.findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc(orderNumberFilter.trim());
        }

        var items = orders.stream()
                .map(BrowseOrderHistory::toItem)
                .toList();

        var result = new BrowseOrderHistoryResponse();
        result.setOrders(items);
        return result;
    }

    private static BrowseOrderHistoryResponse.BrowseOrderHistoryItemResponse toItem(Order order) {
        var item = new BrowseOrderHistoryResponse.BrowseOrderHistoryItemResponse();
        item.setOrderNumber(order.getOrderNumber());
        item.setOrderTimestamp(order.getOrderTimestamp());
        item.setSku(order.getSku());
        item.setCountry(order.getCountry());
        item.setQuantity(order.getQuantity());
        item.setTotalPrice(order.getTotalPrice().amount());
        item.setStatus(order.getStatus());
        item.setAppliedCouponCode(order.getAppliedCouponCode());
        return item;
    }
}
