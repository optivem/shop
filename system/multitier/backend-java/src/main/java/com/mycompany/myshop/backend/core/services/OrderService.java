package com.mycompany.myshop.backend.core.services;

import com.mycompany.myshop.backend.core.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.core.dtos.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.backend.core.dtos.RecallSkuResponse;
import com.mycompany.myshop.backend.core.entities.Order;
import com.mycompany.myshop.backend.core.entities.OrderStatus;
import com.mycompany.myshop.backend.core.exceptions.NotExistValidationException;
import com.mycompany.myshop.backend.core.exceptions.ValidationException;
import com.mycompany.myshop.backend.core.repositories.OrderRepository;
import com.mycompany.myshop.backend.core.services.external.ClockGateway;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import com.mycompany.myshop.backend.core.services.external.TaxGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.ZoneId;

@Service
public class OrderService {

    private static final int FIRST_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private static final MonthDay YEAR_END_RESTRICTED_MONTH_DAY = MonthDay.of(Month.DECEMBER, 31);
    private static final LocalTime YEAR_END_RESTRICTED_TIME_START = LocalTime.of(23, 59);

    private final OrderRepository orderRepository;
    private final ErpGateway erpGateway;
    private final TaxGateway taxGateway;
    private final ClockGateway clockGateway;
    private final CouponService couponService;

    public OrderService(OrderRepository orderRepository, ErpGateway erpGateway, TaxGateway taxGateway, ClockGateway clockGateway, CouponService couponService) {
        this.orderRepository = orderRepository;
        this.erpGateway = erpGateway;
        this.taxGateway = taxGateway;
        this.clockGateway = clockGateway;
        this.couponService = couponService;
    }

    public PlaceOrderResponse placeOrder(PlaceOrderRequest request) {
        var sku = request.getSku();
        var quantity = request.getQuantity();
        var country = request.getCountry();
        var couponCode = request.getCouponCode();

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
        var promotion = erpGateway.getPromotionDetails();
        var promotionFactor = promotion.isPromotionActive() ? promotion.getDiscount() : BigDecimal.ONE;
        var basePrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        var promotedPrice = basePrice.multiply(promotionFactor);

        var discountRate = couponService.getDiscount(couponCode);
        var discountAmount = promotedPrice.multiply(discountRate);
        var subtotalPrice = promotedPrice.subtract(discountAmount);

        var taxRate = getTaxRate(country);
        var taxAmount = subtotalPrice.multiply(taxRate);
        var totalPrice = subtotalPrice.add(taxAmount);

        var appliedCouponCode = discountRate.compareTo(BigDecimal.ZERO) > 0 ? couponCode : null;

        var orderNumber = generateOrderNumber();

        var order = new Order(orderNumber, orderTimestamp, country,
                sku, quantity, unitPrice, basePrice,
                discountRate, discountAmount, subtotalPrice,
                taxRate, taxAmount, totalPrice, OrderStatus.PLACED,
                appliedCouponCode);

        orderRepository.save(order);

        if (appliedCouponCode != null) {
            couponService.incrementUsageCount(appliedCouponCode);
        }

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

    private BigDecimal getTaxRate(String country) {
        var countryDetails = taxGateway.getTaxDetails(country);
        if (countryDetails.isEmpty()) {
            throw new ValidationException("country", "Country does not exist: " + country);
        }

        return countryDetails.get().getTaxRate();
    }

    public void deliverOrder(String orderNumber) {
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            throw new ValidationException("Order number must not be empty");
        }

        var optionalOrder = orderRepository.findByOrderNumber(orderNumber);

        if (optionalOrder.isEmpty()) {
            throw new NotExistValidationException("Order " + orderNumber + " does not exist.");
        }

        var order = optionalOrder.get();

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new ValidationException("Order cannot be delivered in its current status");
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
    }

    public RecallSkuResponse recallSku(String sku) {
        if (sku == null || sku.trim().isEmpty()) {
            throw new ValidationException("sku", "SKU must not be empty");
        }

        var orders = orderRepository.findBySku(sku);

        var cancelledCount = 0;
        for (var order : orders) {
            if (order.getStatus() == OrderStatus.PLACED) {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                cancelledCount++;
            }
        }

        var response = new RecallSkuResponse();
        response.setSku(sku);
        response.setCancelledCount(cancelledCount);
        return response;
    }

    public void cancelOrder(String orderNumber) {
        var now = LocalDateTime.ofInstant(clockGateway.getCurrentTime(), ZoneId.of("UTC"));
        var currentMonthDay = MonthDay.from(now);

        if (currentMonthDay.equals(YEAR_END_RESTRICTED_MONTH_DAY)) {
            var currentTime = now.toLocalTime();
            var cancelBlackoutStart = LocalTime.of(22, 0);
            var cancelBlackoutEnd = LocalTime.of(22, 30);

            if (!currentTime.isBefore(cancelBlackoutStart) && !currentTime.isAfter(cancelBlackoutEnd)) {
                throw new ValidationException("Order cancellation is not allowed on December 31st between 22:00 and 23:00");
            }
        }

        var optionalOrder = orderRepository.findByOrderNumber(orderNumber);

        if (optionalOrder.isEmpty()) {
            throw new NotExistValidationException("Order " + orderNumber + " does not exist.");
        }

        var order = optionalOrder.get();

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ValidationException("Order has already been cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    public BrowseOrderHistoryResponse browseOrderHistory(String orderNumberFilter, Integer page, Integer size) {
        if (page != null && page < FIRST_PAGE) {
            throw new ValidationException("page", "Page must be " + FIRST_PAGE + " or greater");
        }
        if (size != null && (size < 1 || size > MAX_PAGE_SIZE)) {
            throw new ValidationException("size", "Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        var requestedPage = page == null ? FIRST_PAGE : page;
        var requestedSize = size == null ? DEFAULT_PAGE_SIZE : size;

        java.util.List<Order> orders;
        if (orderNumberFilter == null || orderNumberFilter.trim().isEmpty()) {
            orders = orderRepository.findAllByOrderByOrderTimestampDesc();
        } else {
            orders = orderRepository.findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc(orderNumberFilter.trim());
        }

        // The client asked for one page. The repository has no method that takes one, so every
        // matching row was already read, sorted and hydrated above, and the window is cut here.
        var totalElements = orders.size();
        var from = Math.min((long) (requestedPage - FIRST_PAGE) * requestedSize, totalElements);
        var to = Math.min(from + requestedSize, totalElements);
        var window = orders.subList((int) from, (int) to);

        var items = window.stream()
                .map(order -> {
                    var item = new BrowseOrderHistoryResponse.BrowseOrderHistoryItemResponse();
                    item.setOrderNumber(order.getOrderNumber());
                    item.setOrderTimestamp(order.getOrderTimestamp());
                    item.setSku(order.getSku());
                    item.setCountry(order.getCountry());
                    item.setQuantity(order.getQuantity());
                    item.setTotalPrice(order.getTotalPrice());
                    item.setStatus(order.getStatus());
                    item.setAppliedCouponCode(order.getAppliedCouponCode());
                    return item;
                })
                .toList();

        var result = new BrowseOrderHistoryResponse();
        result.setOrders(items);
        result.setPage(requestedPage);
        result.setSize(requestedSize);
        result.setTotalElements(totalElements);
        result.setTotalPages((int) Math.ceil((double) totalElements / requestedSize));
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
        response.setBasePrice(order.getBasePrice());
        response.setDiscountRate(order.getDiscountRate());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setSubtotalPrice(order.getSubtotalPrice());
        response.setTaxRate(order.getTaxRate());
        response.setTaxAmount(order.getTaxAmount());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus());
        response.setCountry(order.getCountry());
        response.setAppliedCouponCode(order.getAppliedCouponCode());

        return response;
    }

    private String generateOrderNumber() {
        var uuid = java.util.UUID.randomUUID().toString().toUpperCase();
        return "ORD-" + uuid;
    }
}
