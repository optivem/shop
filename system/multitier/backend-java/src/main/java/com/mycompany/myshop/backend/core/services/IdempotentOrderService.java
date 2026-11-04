package com.mycompany.myshop.backend.core.services;

import com.mycompany.myshop.backend.core.dtos.CurrentUser;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * {@code POST /api/orders} with an optional {@code Idempotency-Key}.
 *
 * <p>Deliberately not transactional: the order is placed in {@link OrderService}'s transaction, and
 * this class sits outside it so that it can react to that transaction failing. Two first uses of the
 * same key can race past the replay check; the loser's insert then fails on the unique constraint,
 * its transaction rolls back (no order), and it answers from the winner's row instead.
 */
@Service
public class IdempotentOrderService {

    private final OrderService orderService;
    private final IdempotencyKeyStore idempotencyKeyStore;

    public IdempotentOrderService(OrderService orderService, IdempotencyKeyStore idempotencyKeyStore) {
        this.orderService = orderService;
        this.idempotencyKeyStore = idempotencyKeyStore;
    }

    public IdempotencyKeyStore.StoredResponse<PlaceOrderResponse> placeOrder(PlaceOrderRequest request, CurrentUser user, String idempotencyKey) {
        var key = IdempotencyKeyStore.normalizeKey(idempotencyKey);
        if (key.isEmpty()) {
            return placed(orderService.placeOrder(request, user));
        }

        var claim = IdempotencyKeyStore.claim(key.get(), user, request);
        var replay = idempotencyKeyStore.findReplay(claim, PlaceOrderResponse.class);
        if (replay.isPresent()) {
            return replay.get();
        }

        try {
            return placed(orderService.placeOrder(request, user, claim));
        } catch (DataIntegrityViolationException e) {
            return idempotencyKeyStore.findReplay(claim, PlaceOrderResponse.class).orElseThrow(() -> e);
        }
    }

    private static IdempotencyKeyStore.StoredResponse<PlaceOrderResponse> placed(PlaceOrderResponse response) {
        return new IdempotencyKeyStore.StoredResponse<>(OrderService.PLACE_ORDER_STATUS, response);
    }
}
