package com.mycompany.myshop.backend.core.services;

import com.mycompany.myshop.backend.core.dtos.CurrentUser;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.backend.core.dtos.external.ErpGetPromotionResponse;
import com.mycompany.myshop.backend.core.dtos.external.ErpProductDetailsResponse;
import com.mycompany.myshop.backend.core.dtos.external.TaxDetailsResponse;
import com.mycompany.myshop.backend.core.entities.Order;
import com.mycompany.myshop.backend.core.entities.OrderStatus;
import com.mycompany.myshop.backend.core.exceptions.NotExistValidationException;
import com.mycompany.myshop.backend.core.exceptions.ValidationException;
import com.mycompany.myshop.backend.core.repositories.OrderRepository;
import com.mycompany.myshop.backend.core.services.external.ClockGateway;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import com.mycompany.myshop.backend.core.services.external.TaxGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ErpGateway erpGateway;
    @Mock
    private TaxGateway taxGateway;
    @Mock
    private ClockGateway clockGateway;
    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderService orderService;

    private static final CurrentUser CUSTOMER = new CurrentUser("customer-sub", "customer1", false);
    private static final CurrentUser OTHER_CUSTOMER = new CurrentUser("other-sub", "customer2", false);
    private static final CurrentUser ADMIN = new CurrentUser("admin-sub", "admin1", true);

    private static final Instant NORMAL_TIME = Instant.parse("2025-06-15T10:00:00Z");
    private static final Instant DEC_31_YEAR_END_BLACKOUT = Instant.parse("2025-12-31T23:59:00Z");
    private static final Instant DEC_31_CANCEL_BLACKOUT = Instant.parse("2025-12-31T22:15:00Z");

    @Test
    void placeOrderReturnsOrderNumberStartingWithOrd() {
        givenNormalTime();
        givenProductExists("BOOK-123", new BigDecimal("10.00"));
        givenNoPromotion();
        givenNoDiscount();
        givenTaxRate("US", new BigDecimal("0.10"));

        var response = orderService.placeOrder(buildRequest("BOOK-123", 2, "US"), CUSTOMER);

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertSavedOrder(captor.getValue(), response);
    }

    @Test
    void placeOrderThrowsWhenOrderedOnYearEndBlackout() {
        when(clockGateway.getCurrentTime()).thenReturn(DEC_31_YEAR_END_BLACKOUT);

        var thrown = catchThrowable(() -> orderService.placeOrder(buildRequest("BOOK-123", 1, "US"), CUSTOMER));

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessageContaining("December 31");
    }

    @Test
    void placeOrderThrowsWhenSkuUnknown() {
        givenNormalTime();
        when(erpGateway.getProductDetails("UNKNOWN")).thenReturn(Optional.empty());

        var thrown = catchThrowable(() -> orderService.placeOrder(buildRequest("UNKNOWN", 1, "US"), CUSTOMER));

        assertThat(thrown).isInstanceOf(ValidationException.class);
        assertThat(((ValidationException) thrown).getFieldName()).isEqualTo("sku");
    }

    @Test
    void placeOrderThrowsWhenCountryUnknown() {
        givenNormalTime();
        givenProductExists("BOOK-123", new BigDecimal("10.00"));
        givenNoPromotion();
        givenNoDiscount();
        when(taxGateway.getTaxDetails("XX")).thenReturn(Optional.empty());

        var thrown = catchThrowable(() -> orderService.placeOrder(buildRequest("BOOK-123", 1, "XX"), CUSTOMER));

        assertThat(thrown).isInstanceOf(ValidationException.class);
        assertThat(((ValidationException) thrown).getFieldName()).isEqualTo("country");
    }

    @Test
    void deliverOrderTransitionsStatusToDelivered() {
        var order = placedOrder("ORD-001");
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        orderService.deliverOrder("ORD-001");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(orderRepository).save(order);
    }

    @Test
    void deliverOrderThrowsWhenOrderNotFound() {
        when(orderRepository.findByOrderNumber("ORD-999")).thenReturn(Optional.empty());

        var thrown = catchThrowable(() -> orderService.deliverOrder("ORD-999"));

        assertThat(thrown).isInstanceOf(NotExistValidationException.class);
    }

    @Test
    void deliverOrderThrowsWhenOrderAlreadyDelivered() {
        var order = placedOrder("ORD-001");
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        var thrown = catchThrowable(() -> orderService.deliverOrder("ORD-001"));

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be delivered");
    }

    @Test
    void cancelOrderTransitionsStatusToCancelled() {
        givenNormalTime();
        var order = placedOrder("ORD-001");
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        orderService.cancelOrder("ORD-001", CUSTOMER);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrderThrowsDuringDecember31CancellationBlackout() {
        when(clockGateway.getCurrentTime()).thenReturn(DEC_31_CANCEL_BLACKOUT);

        var thrown = catchThrowable(() -> orderService.cancelOrder("ORD-001", CUSTOMER));

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessageContaining("December 31");
    }

    @Test
    void cancelOrderThrowsWhenOrderAlreadyCancelled() {
        givenNormalTime();
        var order = placedOrder("ORD-001");
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        var thrown = catchThrowable(() -> orderService.cancelOrder("ORD-001", CUSTOMER));

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessageContaining("already been cancelled");
    }

    @Test
    void placeOrderRecordsCallerAsOwner() {
        givenNormalTime();
        givenProductExists("BOOK-123", new BigDecimal("10.00"));
        givenNoPromotion();
        givenNoDiscount();
        givenTaxRate("US", new BigDecimal("0.10"));

        orderService.placeOrder(buildRequest("BOOK-123", 2, "US"), CUSTOMER);

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isEqualTo("customer-sub");
        assertThat(captor.getValue().getOwnerName()).isEqualTo("customer1");
    }

    @Test
    void getOrderReturnsOwnOrderToCustomer() {
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(placedOrder("ORD-001")));

        var response = orderService.getOrder("ORD-001", CUSTOMER);

        assertThat(response.getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    void getOrderHidesAnotherCustomersOrder() {
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(placedOrder("ORD-001")));

        var thrown = catchThrowable(() -> orderService.getOrder("ORD-001", OTHER_CUSTOMER));

        assertThat(thrown).isInstanceOf(NotExistValidationException.class);
    }

    @Test
    void getOrderReturnsAnyOrderToAdmin() {
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(placedOrder("ORD-001")));

        var response = orderService.getOrder("ORD-001", ADMIN);

        assertThat(response.getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    void getOrderWithNoOwnerIsVisibleToAdminOnly() {
        var legacyOrder = placedOrder("ORD-001");
        legacyOrder.setOwner(null);
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(legacyOrder));

        assertThat(orderService.getOrder("ORD-001", ADMIN).getOrderNumber()).isEqualTo("ORD-001");
        assertThat(catchThrowable(() -> orderService.getOrder("ORD-001", CUSTOMER)))
                .isInstanceOf(NotExistValidationException.class);
    }

    @Test
    void cancelOrderHidesAnotherCustomersOrder() {
        givenNormalTime();
        var order = placedOrder("ORD-001");
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        var thrown = catchThrowable(() -> orderService.cancelOrder("ORD-001", OTHER_CUSTOMER));

        assertThat(thrown).isInstanceOf(NotExistValidationException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void cancelOrderAllowsAdminToCancelAnyOrder() {
        givenNormalTime();
        var order = placedOrder("ORD-001");
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        orderService.cancelOrder("ORD-001", ADMIN);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void browseOrderHistoryForCustomerListsOnlyOwnOrders() {
        when(orderRepository.findByOwnerOrderByOrderTimestampDesc("customer-sub"))
                .thenReturn(java.util.List.of(placedOrder("ORD-001")));

        var response = orderService.browseOrderHistory(null, null, null, CUSTOMER);

        assertThat(response.getOrders()).extracting("orderNumber").containsExactly("ORD-001");
        verify(orderRepository, never()).findAllByOrderByOrderTimestampDesc();
    }

    @Test
    void browseOrderHistoryForAdminListsAllOrdersWithCustomer() {
        when(orderRepository.findAllByOrderByOrderTimestampDesc())
                .thenReturn(java.util.List.of(placedOrder("ORD-001")));

        var response = orderService.browseOrderHistory(null, null, null, ADMIN);

        assertThat(response.getOrders()).extracting("customer").containsExactly("customer1");
    }

    private void givenNormalTime() {
        when(clockGateway.getCurrentTime()).thenReturn(NORMAL_TIME);
    }

    private void givenProductExists(String sku, BigDecimal price) {
        var product = new ErpProductDetailsResponse();
        product.setPrice(price);
        when(erpGateway.getProductDetails(sku)).thenReturn(Optional.of(product));
    }

    private void givenNoPromotion() {
        var promotion = new ErpGetPromotionResponse();
        promotion.setPromotionActive(false);
        promotion.setDiscount(BigDecimal.ONE);
        when(erpGateway.getPromotionDetails()).thenReturn(promotion);
    }

    private void givenTaxRate(String country, BigDecimal rate) {
        var taxDetails = new TaxDetailsResponse();
        taxDetails.setTaxRate(rate);
        when(taxGateway.getTaxDetails(country)).thenReturn(Optional.of(taxDetails));
    }

    private void givenNoDiscount() {
        when(couponService.getDiscount(null)).thenReturn(BigDecimal.ZERO);
    }

    private PlaceOrderRequest buildRequest(String sku, int quantity, String country) {
        var request = new PlaceOrderRequest();
        request.setSku(sku);
        request.setQuantity(quantity);
        request.setCountry(country);
        return request;
    }

    private void assertSavedOrder(Order saved, PlaceOrderResponse response) {
        assertThat(saved.getOrderNumber()).startsWith("ORD-").isEqualTo(response.getOrderNumber());
        assertThat(saved.getOrderTimestamp()).isEqualTo(NORMAL_TIME);
        assertThat(saved.getSku()).isEqualTo("BOOK-123");
        assertThat(saved.getQuantity()).isEqualTo(2);
        assertThat(saved.getCountry()).isEqualTo("US");
        assertThat(saved.getUnitPrice()).isEqualByComparingTo("10.00");
        assertThat(saved.getBasePrice()).isEqualByComparingTo("20.00");
        assertThat(saved.getDiscountRate()).isEqualByComparingTo("0");
        assertThat(saved.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(saved.getSubtotalPrice()).isEqualByComparingTo("20.00");
        assertThat(saved.getTaxRate()).isEqualByComparingTo("0.10");
        assertThat(saved.getTaxAmount()).isEqualByComparingTo("2.00");
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("22.00");
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(saved.getAppliedCouponCode()).isNull();
    }

    private Order placedOrder(String orderNumber) {
        return new Order(
                orderNumber, NORMAL_TIME, "US", "BOOK-123", 1,
                new BigDecimal("10.00"), new BigDecimal("10.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10.00"),
                new BigDecimal("0.10"), new BigDecimal("1.00"), new BigDecimal("11.00"),
                OrderStatus.PLACED, null, "customer-sub", "customer1"
        );
    }
}
