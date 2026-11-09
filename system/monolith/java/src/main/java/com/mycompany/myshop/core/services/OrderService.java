package com.mycompany.myshop.core.services;

import com.mycompany.myshop.core.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.core.dtos.CurrentUser;
import com.mycompany.myshop.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.core.dtos.ShippingAddress;
import com.mycompany.myshop.core.dtos.ViewOrderDetailsResponse;
import com.mycompany.myshop.core.entities.Address;
import com.mycompany.myshop.core.entities.Order;
import com.mycompany.myshop.core.entities.OrderStatus;
import com.mycompany.myshop.core.exceptions.NotExistValidationException;
import com.mycompany.myshop.core.exceptions.ValidationException;
import com.mycompany.myshop.core.repositories.OrderRepository;
import com.mycompany.myshop.core.services.external.ClockGateway;
import com.mycompany.myshop.core.services.external.ErpGateway;
import com.mycompany.myshop.core.services.external.TaxGateway;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    /** The status a successful place-order answers with (201 Created); stored with an idempotency key. */
    public static final int PLACE_ORDER_STATUS = 201;

    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 4;
    private static final MonthDay YEAR_END_RESTRICTED_MONTH_DAY = MonthDay.of(Month.DECEMBER, 31);
    private static final LocalTime YEAR_END_RESTRICTED_TIME_START = LocalTime.of(23, 59);

    private final OrderRepository orderRepository;
    private final ErpGateway erpGateway;
    private final TaxGateway taxGateway;
    private final ClockGateway clockGateway;
    private final CouponService couponService;
    private final IdempotencyKeyStore idempotencyKeyStore;

    public OrderService(OrderRepository orderRepository, ErpGateway erpGateway, TaxGateway taxGateway, ClockGateway clockGateway,
                        CouponService couponService, IdempotencyKeyStore idempotencyKeyStore) {
        this.orderRepository = orderRepository;
        this.erpGateway = erpGateway;
        this.taxGateway = taxGateway;
        this.clockGateway = clockGateway;
        this.couponService = couponService;
        this.idempotencyKeyStore = idempotencyKeyStore;
    }

    @Transactional
    public PlaceOrderResponse placeOrder(PlaceOrderRequest request, CurrentUser user) {
        return placeOrder(request, user, null);
    }

    /**
     * Places the order and, when the request carried an {@code Idempotency-Key}, records the response
     * under that key in the same transaction: either both the order and the key row commit, or
     * neither does.
     */
    @Transactional
    public PlaceOrderResponse placeOrder(PlaceOrderRequest request, CurrentUser user, IdempotencyKeyStore.Claim claim) {
        var sku = request.getSku();
        var quantity = request.getQuantity();
        var couponCode = request.getCouponCode();
        // With a shipping address, its country is the tax country and the top-level one is ignored.
        var shippingAddress = request.getShippingAddress() == null ? null : request.getShippingAddress().trimmed();
        var country = shippingAddress != null ? shippingAddress.getCountry() : request.getCountry();
        var countryField = shippingAddress != null ? "shippingAddress.country" : "country";

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

        var taxRate = getTaxRate(country, countryField);
        var taxAmount = subtotalPrice.multiply(taxRate);
        var totalPrice = subtotalPrice.add(taxAmount);

        var appliedCouponCode = discountRate.compareTo(BigDecimal.ZERO) > 0 ? couponCode : null;

        var orderNumber = generateOrderNumber();

        var order = new Order(orderNumber, orderTimestamp, country,
                sku, quantity, roundMoney(unitPrice), roundMoney(basePrice),
                roundRate(discountRate), roundMoney(discountAmount), roundMoney(subtotalPrice),
                roundRate(taxRate), roundMoney(taxAmount), roundMoney(totalPrice), OrderStatus.PLACED,
                appliedCouponCode, user.subject(), user.username());
        if (shippingAddress != null) {
            order.setShippingAddress(toAddress(shippingAddress));
        }

        var response = new PlaceOrderResponse();
        response.setOrderNumber(orderNumber);

        // The key row goes in first: a concurrent first use of the same key then fails on its unique
        // index before it touches the coupon, and its transaction rolls back with nothing changed.
        if (claim != null) {
            idempotencyKeyStore.record(claim, PLACE_ORDER_STATUS, response);
        }

        // Increment next: a rejected increment leaves no order behind.
        if (appliedCouponCode != null) {
            couponService.incrementUsageCount(appliedCouponCode);
        }

        orderRepository.save(order);

        return response;
    }

    // Intermediate amounts stay exact; each value is rounded exactly once, here, to the scale of the
    // column it is persisted in (NUMERIC(10,2) for money, NUMERIC(5,4) for rates).
    private static BigDecimal roundMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal roundRate(BigDecimal value) {
        return value.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal getUnitPrice(String sku) {
        var productDetails = erpGateway.getProductDetails(sku);
        if (productDetails.isEmpty()) {
            throw new ValidationException("sku", "Product does not exist for SKU: " + sku);
        }

        return productDetails.get().getPrice();
    }

    private BigDecimal getTaxRate(String country, String countryField) {
        var countryDetails = taxGateway.getTaxDetails(country);
        if (countryDetails.isEmpty()) {
            throw new ValidationException(countryField, "Country does not exist: " + country);
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

    public void cancelOrder(String orderNumber, CurrentUser user) {
        var now = LocalDateTime.ofInstant(clockGateway.getCurrentTime(), ZoneId.of("UTC"));
        var currentMonthDay = MonthDay.from(now);

        if (currentMonthDay.equals(YEAR_END_RESTRICTED_MONTH_DAY)) {
            var currentTime = now.toLocalTime();
            var cancelBlackoutStart = LocalTime.of(22, 0);
            var cancelBlackoutEnd = LocalTime.of(22, 30);

            if (!currentTime.isBefore(cancelBlackoutStart) && !currentTime.isAfter(cancelBlackoutEnd)) {
                throw new ValidationException("Order cancellation is not allowed on December 31st between 22:00 and 22:30");
            }
        }

        var order = findAccessibleOrder(orderNumber, user);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ValidationException("Order has already been cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    /**
     * One page of order history. A customer sees only their own orders; an admin sees everyone's,
     * optionally narrowed to one customer (by username, exact match). {@code customerFilter} is
     * ignored for a customer. Either filter blank means no filter.
     */
    @Transactional(readOnly = true)
    public BrowseOrderHistoryResponse browseOrderHistory(String orderNumberFilter, String customerFilter,
                                                         Integer page, Integer size, CurrentUser user) {
        var pageSpec = PageSpec.of(page, size);
        var orderNumber = trimToNull(orderNumberFilter);
        var customer = user.admin() ? trimToNull(customerFilter) : null;

        var totalElements = countOrders(user, customer, orderNumber);

        List<Order> orders = List.of();
        if (pageSpec.reaches(totalElements)) {
            orders = findOrderPage(user, customer, orderNumber, pageSpec.toPageable());
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
                    item.setCustomer(order.getOwnerName());
                    return item;
                })
                .toList();

        var result = new BrowseOrderHistoryResponse();
        result.setOrders(items);
        result.setPage(pageSpec.page());
        result.setSize(pageSpec.size());
        result.setTotalElements(totalElements);
        result.setTotalPages(pageSpec.totalPages(totalElements));
        return result;
    }

    private long countOrders(CurrentUser user, String customer, String orderNumber) {
        if (!user.admin()) {
            return orderNumber != null
                    ? orderRepository.countByOwnerAndOrderNumberContaining(user.subject(), orderNumber)
                    : orderRepository.countByOwner(user.subject());
        }
        if (customer != null) {
            return orderNumber != null
                    ? orderRepository.countByOwnerNameAndOrderNumberContaining(customer, orderNumber)
                    : orderRepository.countByOwnerName(customer);
        }
        return orderNumber != null ? orderRepository.countByOrderNumberContaining(orderNumber) : orderRepository.count();
    }

    private List<Order> findOrderPage(CurrentUser user, String customer, String orderNumber, Pageable pageable) {
        if (!user.admin()) {
            return orderNumber != null
                    ? orderRepository.findPageByOwnerAndOrderNumberContaining(user.subject(), orderNumber, pageable)
                    : orderRepository.findPageByOwner(user.subject(), pageable);
        }
        if (customer != null) {
            return orderNumber != null
                    ? orderRepository.findPageByOwnerNameAndOrderNumberContaining(customer, orderNumber, pageable)
                    : orderRepository.findPageByOwnerName(customer, pageable);
        }
        return orderNumber != null
                ? orderRepository.findPageByOrderNumberContaining(orderNumber, pageable)
                : orderRepository.findPage(pageable);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public ViewOrderDetailsResponse getOrder(String orderNumber, CurrentUser user) {
        var order = findAccessibleOrder(orderNumber, user);

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
        response.setShippingAddress(toShippingAddress(order.getShippingAddress()));

        return response;
    }

    /** Another customer's order is reported as non-existent, so its existence is not revealed. */
    private Order findAccessibleOrder(String orderNumber, CurrentUser user) {
        return orderRepository.findByOrderNumber(orderNumber)
                .filter(order -> user.admin() || user.subject().equals(order.getOwner()))
                .orElseThrow(() -> new NotExistValidationException("Order " + orderNumber + " does not exist."));
    }

    private static ShippingAddress toShippingAddress(Address address) {
        if (address == null) {
            return null;
        }
        return new ShippingAddress(address.getFirstName(), address.getLastName(), address.getStreet(),
                address.getPostalCode(), address.getCity(), address.getCountry());
    }

    private static Address toAddress(ShippingAddress address) {
        return new Address(address.getFirstName(), address.getLastName(), address.getStreet(),
                address.getPostalCode(), address.getCity(), address.getCountry());
    }

    private String generateOrderNumber() {
        var uuid = UUID.randomUUID().toString().toUpperCase();
        return "ORD-" + uuid;
    }
}
