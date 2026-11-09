package com.mycompany.myshop.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myshop.core.dtos.CurrentUser;
import com.mycompany.myshop.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.core.dtos.ShippingAddress;
import com.mycompany.myshop.core.dtos.external.ErpGetPromotionResponse;
import com.mycompany.myshop.core.dtos.external.ErpProductDetailsResponse;
import com.mycompany.myshop.core.dtos.external.TaxDetailsResponse;
import com.mycompany.myshop.core.entities.Address;
import com.mycompany.myshop.core.entities.Order;
import com.mycompany.myshop.core.entities.OrderStatus;
import com.mycompany.myshop.core.exceptions.NotExistValidationException;
import com.mycompany.myshop.core.exceptions.ValidationException;
import com.mycompany.myshop.core.repositories.OrderRepository;
import com.mycompany.myshop.core.services.external.ClockGateway;
import com.mycompany.myshop.core.services.external.ErpGateway;
import com.mycompany.myshop.core.services.external.TaxGateway;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final CurrentUser CUSTOMER = new CurrentUser("customer-sub", "customer1", false);
    private static final CurrentUser OTHER_CUSTOMER = new CurrentUser("other-sub", "customer2", false);
    private static final CurrentUser ADMIN = new CurrentUser("admin-sub", "admin1", true);

    private static final Instant NORMAL_TIME = Instant.parse("2025-06-15T10:00:00Z");

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
    @Mock
    private IdempotencyKeyStore idempotencyKeyStore;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrderRecordsCallerAsOwner() {
        when(clockGateway.getCurrentTime()).thenReturn(NORMAL_TIME);
        var product = new ErpProductDetailsResponse();
        product.setPrice(new BigDecimal("10.00"));
        when(erpGateway.getProductDetails("BOOK-123")).thenReturn(Optional.of(product));
        var promotion = new ErpGetPromotionResponse();
        promotion.setPromotionActive(false);
        promotion.setDiscount(BigDecimal.ONE);
        when(erpGateway.getPromotionDetails()).thenReturn(promotion);
        when(couponService.getDiscount(null)).thenReturn(BigDecimal.ZERO);
        var taxDetails = new TaxDetailsResponse();
        taxDetails.setTaxRate(new BigDecimal("0.10"));
        when(taxGateway.getTaxDetails("US")).thenReturn(Optional.of(taxDetails));

        var request = new PlaceOrderRequest();
        request.setSku("BOOK-123");
        request.setQuantity(2);
        request.setCountry("US");
        orderService.placeOrder(request, CUSTOMER);

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isEqualTo("customer-sub");
        assertThat(captor.getValue().getOwnerName()).isEqualTo("customer1");
    }

    @Test
    void getOrderReturnsOwnOrderToCustomer() {
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(placedOrder("ORD-001", "customer-sub")));

        var response = orderService.getOrder("ORD-001", CUSTOMER);

        assertThat(response.getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    void getOrderHidesAnotherCustomersOrder() {
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(placedOrder("ORD-001", "customer-sub")));

        var thrown = catchThrowable(() -> orderService.getOrder("ORD-001", OTHER_CUSTOMER));

        assertThat(thrown).isInstanceOf(NotExistValidationException.class);
    }

    @Test
    void getOrderReturnsAnyOrderToAdmin() {
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(placedOrder("ORD-001", "customer-sub")));

        var response = orderService.getOrder("ORD-001", ADMIN);

        assertThat(response.getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    void getOrderWithNoOwnerIsVisibleToAdminOnly() {
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(placedOrder("ORD-001", null)));

        assertThat(orderService.getOrder("ORD-001", ADMIN).getOrderNumber()).isEqualTo("ORD-001");
        assertThat(catchThrowable(() -> orderService.getOrder("ORD-001", CUSTOMER)))
                .isInstanceOf(NotExistValidationException.class);
    }

    @Test
    void cancelOrderHidesAnotherCustomersOrder() {
        when(clockGateway.getCurrentTime()).thenReturn(NORMAL_TIME);
        var order = placedOrder("ORD-001", "customer-sub");
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        var thrown = catchThrowable(() -> orderService.cancelOrder("ORD-001", OTHER_CUSTOMER));

        assertThat(thrown).isInstanceOf(NotExistValidationException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void cancelOrderAllowsOwnerToCancelOwnOrder() {
        when(clockGateway.getCurrentTime()).thenReturn(NORMAL_TIME);
        var order = placedOrder("ORD-001", "customer-sub");
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        orderService.cancelOrder("ORD-001", CUSTOMER);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrderAllowsAdminToCancelAnyOrder() {
        when(clockGateway.getCurrentTime()).thenReturn(NORMAL_TIME);
        var order = placedOrder("ORD-001", "customer-sub");
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        orderService.cancelOrder("ORD-001", ADMIN);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void browseOrderHistoryForCustomerListsOnlyOwnOrders() {
        when(orderRepository.countByOwner("customer-sub")).thenReturn(1L);
        when(orderRepository.findPageByOwner(eq("customer-sub"), any(Pageable.class)))
                .thenReturn(List.of(placedOrder("ORD-001", "customer-sub")));

        var response = orderService.browseOrderHistory(null, null, null, null, CUSTOMER);

        assertThat(response.getOrders()).extracting("orderNumber").containsExactly("ORD-001");
        verify(orderRepository, never()).findPage(any(Pageable.class));
    }

    @Test
    void browseOrderHistoryForCustomerWithFilterListsOnlyOwnMatchingOrders() {
        when(orderRepository.countByOwnerAndOrderNumberContaining("customer-sub", "001")).thenReturn(1L);
        when(orderRepository.findPageByOwnerAndOrderNumberContaining(eq("customer-sub"), eq("001"), any(Pageable.class)))
                .thenReturn(List.of(placedOrder("ORD-001", "customer-sub")));

        var response = orderService.browseOrderHistory(" 001 ", null, null, null, CUSTOMER);

        assertThat(response.getOrders()).extracting("orderNumber").containsExactly("ORD-001");
        verify(orderRepository, never()).findPageByOrderNumberContaining(eq("001"), any(Pageable.class));
    }

    @Test
    void browseOrderHistoryForAdminListsAllOrdersWithCustomer() {
        when(orderRepository.count()).thenReturn(1L);
        when(orderRepository.findPage(any(Pageable.class)))
                .thenReturn(List.of(placedOrder("ORD-001", "customer-sub")));

        var response = orderService.browseOrderHistory(null, null, null, null, ADMIN);

        assertThat(response.getOrders()).extracting("customer").containsExactly("customer1");
    }

    @Test
    void browseOrderHistoryReadsTheRequestedPageAndReportsTheTotals() {
        when(orderRepository.count()).thenReturn(26L);
        when(orderRepository.findPage(any(Pageable.class))).thenReturn(List.of(placedOrder("ORD-021", "customer-sub")));

        var response = orderService.browseOrderHistory(null, null, 3, 10, ADMIN);

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findPage(pageable.capture());
        assertThat(pageable.getValue().getOffset()).isEqualTo(20);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(response.getPage()).isEqualTo(3);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(26);
        assertThat(response.getTotalPages()).isEqualTo(3);
    }

    @Test
    void browseOrderHistoryPastTheLastPageIsEmptyWithoutReadingRows() {
        when(orderRepository.count()).thenReturn(3L);

        var response = orderService.browseOrderHistory(null, null, 2_000_000_000, 100, ADMIN);

        assertThat(response.getOrders()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(3);
        verify(orderRepository, never()).findPage(any(Pageable.class));
    }

    @Test
    void browseOrderHistoryRejectsASizeAboveTheCap() {
        var thrown = catchThrowable(() -> orderService.browseOrderHistory(null, null, null, 101, ADMIN));

        assertThat(thrown).isInstanceOf(ValidationException.class).hasMessage("Page size must be between 1 and 100");
        assertThat(((ValidationException) thrown).getFieldName()).isEqualTo("size");
    }

    @Test
    void placeOrderWithShippingAddressTaxesByAddressCountryAndStoresTrimmedAddress() {
        stubPricing("BOOK-123", new BigDecimal("50.00"));
        var taxDetails = new TaxDetailsResponse();
        taxDetails.setTaxRate(new BigDecimal("0.19"));
        when(taxGateway.getTaxDetails("DE")).thenReturn(Optional.of(taxDetails));

        var request = new PlaceOrderRequest();
        request.setSku("BOOK-123");
        request.setQuantity(1);
        request.setCountry("US");
        request.setShippingAddress(new ShippingAddress(" Ana ", "Novak", "Main St 1 ", "10115", "Berlin", " DE "));
        orderService.placeOrder(request, CUSTOMER);

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        var order = captor.getValue();
        assertThat(order.getCountry()).isEqualTo("DE");
        assertThat(order.getTaxRate()).isEqualByComparingTo("0.19");
        assertThat(order.getTotalPrice()).isEqualByComparingTo("59.50");
        assertThat(order.getShippingAddress()).isEqualTo(new Address("Ana", "Novak", "Main St 1", "10115", "Berlin", "DE"));
    }

    @Test
    void placeOrderWithoutShippingAddressStoresNoAddress() {
        stubPricing("BOOK-123", new BigDecimal("10.00"));
        var taxDetails = new TaxDetailsResponse();
        taxDetails.setTaxRate(new BigDecimal("0.10"));
        when(taxGateway.getTaxDetails("US")).thenReturn(Optional.of(taxDetails));

        var request = new PlaceOrderRequest();
        request.setSku("BOOK-123");
        request.setQuantity(1);
        request.setCountry("US");
        orderService.placeOrder(request, CUSTOMER);

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getShippingAddress()).isNull();
    }

    @Test
    void placeOrderWithUnknownShippingCountryReportsTheAddressField() {
        stubPricing("BOOK-123", new BigDecimal("10.00"));
        when(taxGateway.getTaxDetails("XX")).thenReturn(Optional.empty());

        var request = new PlaceOrderRequest();
        request.setSku("BOOK-123");
        request.setQuantity(1);
        request.setShippingAddress(new ShippingAddress("Ana", "Novak", "Main St 1", "10115", "Berlin", "XX"));
        var thrown = catchThrowableOfType(ValidationException.class, () -> orderService.placeOrder(request, CUSTOMER));

        assertThat(thrown.getFieldName()).isEqualTo("shippingAddress.country");
        assertThat(thrown).hasMessage("Country does not exist: XX");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderReturnsTheShippingAddressOrNull() {
        var withAddress = placedOrder("ORD-001", "customer-sub");
        withAddress.setShippingAddress(new Address("Ana", "Novak", "Main St 1", "10115", "Berlin", "DE"));
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(withAddress));
        when(orderRepository.findByOrderNumber("ORD-002")).thenReturn(Optional.of(placedOrder("ORD-002", "customer-sub")));

        assertThat(orderService.getOrder("ORD-001", CUSTOMER).getShippingAddress())
                .isEqualTo(new ShippingAddress("Ana", "Novak", "Main St 1", "10115", "Berlin", "DE"));
        assertThat(orderService.getOrder("ORD-002", CUSTOMER).getShippingAddress()).isNull();
    }

    private void stubPricing(String sku, BigDecimal price) {
        when(clockGateway.getCurrentTime()).thenReturn(NORMAL_TIME);
        var product = new ErpProductDetailsResponse();
        product.setPrice(price);
        when(erpGateway.getProductDetails(sku)).thenReturn(Optional.of(product));
        var promotion = new ErpGetPromotionResponse();
        promotion.setPromotionActive(false);
        promotion.setDiscount(BigDecimal.ONE);
        when(erpGateway.getPromotionDetails()).thenReturn(promotion);
        when(couponService.getDiscount(null)).thenReturn(BigDecimal.ZERO);
    }

    private Order placedOrder(String orderNumber, String owner) {
        return new Order(
                orderNumber, NORMAL_TIME, "US", "BOOK-123", 1,
                new BigDecimal("10.00"), new BigDecimal("10.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10.00"),
                new BigDecimal("0.10"), new BigDecimal("1.00"), new BigDecimal("11.00"),
                OrderStatus.PLACED, null, owner, owner == null ? null : "customer1");
    }
}
