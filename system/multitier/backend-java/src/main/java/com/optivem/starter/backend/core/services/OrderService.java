package com.optivem.starter.backend.core.services;

import com.optivem.starter.backend.core.dtos.BrowseOrderHistoryResponse;
import com.optivem.starter.backend.core.dtos.ViewOrderDetailsResponse;
import com.optivem.starter.backend.core.dtos.PlaceOrderRequest;
import com.optivem.starter.backend.core.dtos.PlaceOrderResponse;
import com.optivem.starter.backend.core.entities.Order;
import com.optivem.starter.backend.core.entities.OrderStatus;
import com.optivem.starter.backend.core.exceptions.NotExistValidationException;
import com.optivem.starter.backend.core.exceptions.ValidationException;
import com.optivem.starter.backend.core.repositories.OrderRepository;
import com.optivem.starter.backend.core.services.external.ClockGateway;
import com.optivem.starter.backend.core.services.external.ErpGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ErpGateway erpGateway;
    private final ClockGateway clockGateway;

    public OrderService(OrderRepository orderRepository, ErpGateway erpGateway, ClockGateway clockGateway) {
        this.orderRepository = orderRepository;
        this.erpGateway = erpGateway;
        this.clockGateway = clockGateway;
    }

    public PlaceOrderResponse placeOrder(PlaceOrderRequest request) {
        var sku = request.getSku();
        var quantity = request.getQuantity();

        var orderTimestamp = clockGateway.getCurrentTime();
        var unitPrice = getUnitPrice(sku);
        var totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

        var orderNumber = generateOrderNumber();

        var order = new Order(orderNumber, orderTimestamp,
                sku, quantity, unitPrice, totalPrice, OrderStatus.PLACED);

        orderRepository.save(order);

        var response = new PlaceOrderResponse();
        response.setOrderNumber(orderNumber);
        return response;
    }

    private BigDecimal getUnitPrice(String sku) {
        var productDetails = erpGateway.getProductDetails(sku);
        if (productDetails.isEmpty()) {
            throw new ValidationException("sku", "Product does not exist for SKU: " + sku);
        }

        return productDetails.get().getPrice();
    }

    public BrowseOrderHistoryResponse browseOrderHistory(String orderNumberFilter) {
        java.util.List<Order> orders;
        if (orderNumberFilter == null || orderNumberFilter.trim().isEmpty()) {
            orders = orderRepository.findAllByOrderByOrderTimestampDesc();
        } else {
            orders = orderRepository.findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc(orderNumberFilter.trim());
        }

        var items = orders.stream()
                .map(order -> {
                    var item = new BrowseOrderHistoryResponse.BrowseOrderHistoryItemResponse();
                    item.setOrderNumber(order.getOrderNumber());
                    item.setOrderTimestamp(order.getOrderTimestamp());
                    item.setSku(order.getSku());
                    item.setQuantity(order.getQuantity());
                    item.setTotalPrice(order.getTotalPrice());
                    item.setStatus(order.getStatus());
                    return item;
                })
                .collect(Collectors.toList());

        var result = new BrowseOrderHistoryResponse();
        result.setOrders(items);
        return result;
    }

    public ViewOrderDetailsResponse getOrder(String orderNumber) {
        var optionalOrder = orderRepository.findByOrderNumber(orderNumber);

        if (optionalOrder.isEmpty()) {
            throw new NotExistValidationException("Order " + orderNumber + " does not exist.");
        }

        var order = optionalOrder.get();

        var response = new ViewOrderDetailsResponse();
        response.setOrderNumber(orderNumber);
        response.setOrderTimestamp(order.getOrderTimestamp());
        response.setSku(order.getSku());
        response.setQuantity(order.getQuantity());
        response.setUnitPrice(order.getUnitPrice());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus());

        return response;
    }

    private String generateOrderNumber() {
        var uuid = java.util.UUID.randomUUID().toString().toUpperCase();
        return "ORD-" + uuid;
    }
}
