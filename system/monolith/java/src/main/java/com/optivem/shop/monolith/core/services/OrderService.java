package com.optivem.shop.monolith.core.services;

import com.optivem.shop.monolith.core.dtos.BrowseOrderHistoryResponse;
import com.optivem.shop.monolith.core.dtos.PlaceOrderRequest;
import com.optivem.shop.monolith.core.dtos.PlaceOrderResponse;
import com.optivem.shop.monolith.core.dtos.ViewOrderDetailsResponse;
import com.optivem.shop.monolith.core.entities.Order;
import com.optivem.shop.monolith.core.entities.OrderStatus;
import com.optivem.shop.monolith.core.exceptions.NotExistValidationException;
import com.optivem.shop.monolith.core.exceptions.ValidationException;
import com.optivem.shop.monolith.core.repositories.OrderRepository;
import com.optivem.shop.monolith.core.services.external.ClockGateway;
import com.optivem.shop.monolith.core.services.external.ErpGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final MonthDay YEAR_END_RESTRICTED_MONTH_DAY = MonthDay.of(12, 31);
    private static final LocalTime YEAR_END_RESTRICTED_TIME_START = LocalTime.of(23, 59);

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

        var now = LocalDateTime.ofInstant(orderTimestamp, ZoneId.of("UTC"));
        var currentMonthDay = MonthDay.from(now);

        if (currentMonthDay.equals(YEAR_END_RESTRICTED_MONTH_DAY)) {
            var currentTime = now.toLocalTime();

            if (!currentTime.isBefore(YEAR_END_RESTRICTED_TIME_START)) {
                throw new ValidationException("Orders cannot be placed between 23:59 and 00:00 on December 31st");
            }
        }
        var unitPrice = getUnitPrice(sku);
        var dayOfWeek = now.getDayOfWeek();
        var isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        var discountFactor = isWeekend ? BigDecimal.valueOf(0.5) : BigDecimal.ONE;
        var totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity)).multiply(discountFactor);

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
        List<Order> orders;
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
                .toList();

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
        var uuid = UUID.randomUUID().toString().toUpperCase();
        return "ORD-" + uuid;
    }
}
