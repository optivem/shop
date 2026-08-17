package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.entities.OrderStatus;
import com.mycompany.myshop.backend.domain.exceptions.NotExistValidationException;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.usecases.coupon.CouponService;
import com.mycompany.myshop.backend.usecases.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.usecases.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.usecases.dtos.PlaceOrderResponse;
import com.mycompany.myshop.backend.usecases.dtos.ViewOrderDetailsResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Still one grab-bag service: Step 6 of the refactor plan splits it into {@code PlaceOrder},
 * {@code CancelOrder}, {@code DeliverOrder}, {@code ViewOrderDetails} and {@code BrowseOrderHistory},
 * and Step 8 pushes the status guards and the pricing arithmetic down into the domain. What has
 * changed already is which way the dependencies point: every collaborator below is a domain port.
 */
@Service
public class OrderService {

    private static final MonthDay YEAR_END_RESTRICTED_MONTH_DAY = MonthDay.of(Month.DECEMBER, 31);
    private static final LocalTime YEAR_END_RESTRICTED_TIME_START = LocalTime.of(23, 59);

    private final OrderRepository orderRepository;
    private final ErpGateway erpGateway;
    private final TaxGateway taxGateway;
    private final ClockGateway clockGateway;
    private final CouponService couponService;

    public OrderService(OrderRepository orderRepository, ErpGateway erpGateway, TaxGateway taxGateway,
                        ClockGateway clockGateway, CouponService couponService) {
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
        var promotionFactor = erpGateway.getPromotionDetails().factor();
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
        var product = erpGateway.getProductDetails(sku);
        if (product.isEmpty()) {
            throw new ValidationException("sku", "Product does not exist for SKU: " + sku);
        }

        return product.get().getPrice();
    }

    private BigDecimal getTaxRate(String country) {
        var taxRate = taxGateway.getTaxDetails(country);
        if (taxRate.isEmpty()) {
            throw new ValidationException("country", "Country does not exist: " + country);
        }

        return taxRate.get().getRate();
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
        var uuid = UUID.randomUUID().toString().toUpperCase();
        return "ORD-" + uuid;
    }
}
