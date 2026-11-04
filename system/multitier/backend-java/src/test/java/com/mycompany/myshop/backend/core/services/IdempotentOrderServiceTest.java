package com.mycompany.myshop.backend.core.services;

import com.mycompany.myshop.backend.core.dtos.CurrentUser;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentOrderServiceTest {

    private static final CurrentUser CUSTOMER = new CurrentUser("customer-sub", "customer1", false);

    @Mock
    private OrderService orderService;
    @Mock
    private IdempotencyKeyStore idempotencyKeyStore;

    @InjectMocks
    private IdempotentOrderService idempotentOrderService;

    @Test
    void withoutKeyPlacesTheOrderAndStoresNothing() {
        var request = request();
        when(orderService.placeOrder(request, CUSTOMER)).thenReturn(response("ORD-1"));

        var result = idempotentOrderService.placeOrder(request, CUSTOMER, " ");

        assertThat(result.status()).isEqualTo(201);
        assertThat(result.body().getOrderNumber()).isEqualTo("ORD-1");
        verify(idempotencyKeyStore, never()).findReplay(any(), any());
    }

    @Test
    void firstUseOfAKeyPlacesTheOrderUnderItsClaim() {
        var request = request();
        var claim = IdempotencyKeyStore.claim("key-1", CUSTOMER, request);
        when(idempotencyKeyStore.findReplay(claim, PlaceOrderResponse.class)).thenReturn(Optional.empty());
        when(orderService.placeOrder(request, CUSTOMER, claim)).thenReturn(response("ORD-1"));

        var result = idempotentOrderService.placeOrder(request, CUSTOMER, "key-1");

        assertThat(result.body().getOrderNumber()).isEqualTo("ORD-1");
    }

    @Test
    void usedKeyReplaysWithoutPlacingAnOrder() {
        var request = request();
        when(idempotencyKeyStore.findReplay(any(), eq(PlaceOrderResponse.class)))
                .thenReturn(Optional.of(new IdempotencyKeyStore.StoredResponse<>(201, response("ORD-1"))));

        var result = idempotentOrderService.placeOrder(request, CUSTOMER, "key-1");

        assertThat(result.body().getOrderNumber()).isEqualTo("ORD-1");
        verify(orderService, never()).placeOrder(any(), any(), any());
    }

    @Test
    void losingAConcurrentFirstUseAnswersFromTheWinnersRow() {
        var request = request();
        when(idempotencyKeyStore.findReplay(any(), eq(PlaceOrderResponse.class)))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new IdempotencyKeyStore.StoredResponse<>(201, response("ORD-WINNER"))));
        when(orderService.placeOrder(eq(request), eq(CUSTOMER), any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        var result = idempotentOrderService.placeOrder(request, CUSTOMER, "key-1");

        assertThat(result.body().getOrderNumber()).isEqualTo("ORD-WINNER");
    }

    @Test
    void constraintViolationWithoutAStoredRowIsRethrown() {
        var request = request();
        when(idempotencyKeyStore.findReplay(any(), eq(PlaceOrderResponse.class))).thenReturn(Optional.empty());
        var failure = new DataIntegrityViolationException("something else");
        when(orderService.placeOrder(eq(request), eq(CUSTOMER), any())).thenThrow(failure);

        assertThat(catchThrowable(() -> idempotentOrderService.placeOrder(request, CUSTOMER, "key-1"))).isSameAs(failure);
    }

    private static PlaceOrderRequest request() {
        var request = new PlaceOrderRequest();
        request.setSku("BOOK-123");
        request.setQuantity(1);
        request.setCountry("US");
        return request;
    }

    private static PlaceOrderResponse response(String orderNumber) {
        var response = new PlaceOrderResponse();
        response.setOrderNumber(orderNumber);
        return response;
    }
}
