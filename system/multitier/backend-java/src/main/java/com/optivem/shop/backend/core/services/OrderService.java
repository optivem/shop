package com.optivem.shop.backend.core.services;

import com.optivem.shop.backend.core.dtos.BrowseOrderHistoryResponse;
import com.optivem.shop.backend.core.dtos.ViewOrderDetailsResponse;
import com.optivem.shop.backend.core.dtos.PlaceOrderRequest;
import com.optivem.shop.backend.core.dtos.PlaceOrderResponse;
import com.optivem.shop.backend.core.entities.Order;
import com.optivem.shop.backend.core.entities.OrderStatus;
import com.optivem.shop.backend.core.exceptions.NotExistValidationException;
import com.optivem.shop.backend.core.exceptions.ValidationException;
import com.optivem.shop.backend.core.repositories.OrderRepository;
import com.optivem.shop.backend.core.services.external.ClockGateway;
import com.optivem.shop.backend.core.services.external.ErpGateway;
import com.optivem.shop.backend.core.services.external.TaxGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.ZoneId;

@Service
public class OrderService {

    private static final MonthDay YEAR_END_RESTRICTED_MONTH_DAY = MonthDay.of(12, 31);
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
        var country = (request.getCountry() != null && !request.getCountry().isBlank()) ? request.getCountry() : "US";
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
        var basePrice = unitPrice.multiply(BigDecimal.valueOf(quantity)).multiply(promotionFactor);

        var discountRate = couponService.getDiscount(couponCode);
        var discountAmount = basePrice.multiply(discountRate);
        var subtotalPrice = basePrice.subtract(discountAmount);

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
