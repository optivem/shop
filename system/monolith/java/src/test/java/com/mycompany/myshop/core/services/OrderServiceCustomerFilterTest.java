package com.mycompany.myshop.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myshop.core.dtos.CurrentUser;
import com.mycompany.myshop.core.entities.Order;
import com.mycompany.myshop.core.entities.OrderStatus;
import com.mycompany.myshop.core.repositories.OrderRepository;
import com.mycompany.myshop.core.services.external.ClockGateway;
import com.mycompany.myshop.core.services.external.ErpGateway;
import com.mycompany.myshop.core.services.external.TaxGateway;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/** The admin customer filter on order history (UC-011): exact username match, ignored for a customer. */
@ExtendWith(MockitoExtension.class)
class OrderServiceCustomerFilterTest {

    private static final CurrentUser CUSTOMER = new CurrentUser("customer-sub", "customer1", false);
    private static final CurrentUser ADMIN = new CurrentUser("admin-sub", "admin1", true);

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
    void browseOrderHistoryForAdminWithCustomerFilterListsOnlyThatCustomersOrders() {
        when(orderRepository.countByOwnerNameAndOrderNumberContaining("customer1", "001")).thenReturn(1L);
        when(orderRepository.findPageByOwnerNameAndOrderNumberContaining(eq("customer1"), eq("001"), any(Pageable.class)))
                .thenReturn(List.of(placedOrder("ORD-001")));

        var response = orderService.browseOrderHistory("001", " customer1 ", null, null, ADMIN);

        assertThat(response.getOrders()).extracting("orderNumber").containsExactly("ORD-001");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void browseOrderHistoryForAdminWithOnlyCustomerFilter() {
        when(orderRepository.countByOwnerName("customer1")).thenReturn(0L);

        var response = orderService.browseOrderHistory(null, "customer1", null, null, ADMIN);

        assertThat(response.getOrders()).isEmpty();
        assertThat(response.getTotalPages()).isZero();
        verify(orderRepository, never()).count();
    }

    @Test
    void browseOrderHistoryForAdminWithBlankCustomerFilterListsAllOrders() {
        when(orderRepository.count()).thenReturn(0L);

        orderService.browseOrderHistory(null, "  ", null, null, ADMIN);

        verify(orderRepository, never()).countByOwnerName(any());
    }

    @Test
    void browseOrderHistoryIgnoresCustomerFilterForCustomer() {
        when(orderRepository.countByOwner("customer-sub")).thenReturn(1L);
        when(orderRepository.findPageByOwner(eq("customer-sub"), any(Pageable.class)))
                .thenReturn(List.of(placedOrder("ORD-001")));

        var response = orderService.browseOrderHistory(null, "customer2", null, null, CUSTOMER);

        assertThat(response.getOrders()).extracting("orderNumber").containsExactly("ORD-001");
        verify(orderRepository, never()).countByOwnerName(any());
    }

    private static Order placedOrder(String orderNumber) {
        return new Order(
                orderNumber, Instant.parse("2025-06-15T10:00:00Z"), "US", "BOOK-123", 1,
                new BigDecimal("10.00"), new BigDecimal("10.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10.00"),
                new BigDecimal("0.10"), new BigDecimal("1.00"), new BigDecimal("11.00"),
                OrderStatus.PLACED, null, "customer-sub", "customer1");
    }
}
