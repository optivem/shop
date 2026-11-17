package com.mycompany.myshop.backend.core.services;

import com.mycompany.myshop.backend.core.dtos.CurrentUser;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.core.dtos.external.ErpGetPromotionResponse;
import com.mycompany.myshop.backend.core.dtos.external.ErpProductDetailsResponse;
import com.mycompany.myshop.backend.core.dtos.external.TaxDetailsResponse;
import com.mycompany.myshop.backend.core.outbox.OrderPlaced;
import com.mycompany.myshop.backend.core.outbox.OutboxStore;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Placing an order publishes OrderPlaced through the transactional outbox; a rejected one publishes nothing. */
@ExtendWith(MockitoExtension.class)
class OrderServiceOutboxTest {

    private static final CurrentUser CUSTOMER = new CurrentUser("customer-sub", "customer1", false);
    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");

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
    @Mock
    private OutboxStore outboxStore;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrderWritesOrderPlacedEventWithTheStoredTotal() {
        when(clockGateway.getCurrentTime()).thenReturn(NOW);
        var product = new ErpProductDetailsResponse();
        product.setPrice(new BigDecimal("10.00"));
        when(erpGateway.getProductDetails("BOOK-123")).thenReturn(Optional.of(product));
        var promotion = new ErpGetPromotionResponse();
        promotion.setPromotionActive(false);
        promotion.setDiscount(BigDecimal.ONE);
        when(erpGateway.getPromotionDetails()).thenReturn(promotion);
        when(couponService.getDiscount(null)).thenReturn(BigDecimal.ZERO);
        var tax = new TaxDetailsResponse();
        tax.setTaxRate(new BigDecimal("0.10"));
        when(taxGateway.getTaxDetails("US")).thenReturn(Optional.of(tax));

        var response = orderService.placeOrder(request("BOOK-123"), CUSTOMER);

        var payload = ArgumentCaptor.forClass(Object.class);
        verify(outboxStore).append(eq(OrderPlaced.AGGREGATE_TYPE), eq(response.getOrderNumber()),
                eq(OrderPlaced.EVENT_TYPE), payload.capture());
        assertThat(payload.getValue()).isEqualTo(
                new OrderPlaced(response.getOrderNumber(), "BOOK-123", 2, new BigDecimal("22.00"), NOW));
    }

    @Test
    void rejectedOrderWritesNoEvent() {
        when(clockGateway.getCurrentTime()).thenReturn(NOW);
        when(erpGateway.getProductDetails("UNKNOWN")).thenReturn(Optional.empty());

        catchThrowable(() -> orderService.placeOrder(request("UNKNOWN"), CUSTOMER));

        verify(outboxStore, never()).append(any(), any(), any(), any());
    }

    private static PlaceOrderRequest request(String sku) {
        var request = new PlaceOrderRequest();
        request.setSku(sku);
        request.setQuantity(2);
        request.setCountry("US");
        return request;
    }
}
