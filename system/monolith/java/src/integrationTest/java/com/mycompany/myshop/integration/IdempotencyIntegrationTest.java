package com.mycompany.myshop.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.when;

import com.mycompany.myshop.AbstractIntegrationTest;
import com.mycompany.myshop.core.dtos.CurrentUser;
import com.mycompany.myshop.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.core.dtos.external.ErpGetPromotionResponse;
import com.mycompany.myshop.core.dtos.external.ErpProductDetailsResponse;
import com.mycompany.myshop.core.dtos.external.TaxDetailsResponse;
import com.mycompany.myshop.core.entities.IdempotencyKey;
import com.mycompany.myshop.core.exceptions.ValidationException;
import com.mycompany.myshop.core.repositories.CouponRepository;
import com.mycompany.myshop.core.repositories.IdempotencyKeyRepository;
import com.mycompany.myshop.core.repositories.OrderRepository;
import com.mycompany.myshop.core.services.CouponService;
import com.mycompany.myshop.core.services.IdempotencyKeyStore;
import com.mycompany.myshop.core.services.IdempotentOrderService;
import com.mycompany.myshop.core.services.external.ClockGateway;
import com.mycompany.myshop.core.services.external.ErpGateway;
import com.mycompany.myshop.core.services.external.TaxGateway;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// The idempotency guarantees that need a real database: the unique constraint under concurrent first
// uses, the transaction shared by the order and the key row, and expiry. Single-call behaviour
// (replay, conflicting replay, no key) is covered by the acceptance scenarios.
@SpringBootTest
@ActiveProfiles("test")
class IdempotencyIntegrationTest extends AbstractIntegrationTest {

    private static final CurrentUser CUSTOMER = new CurrentUser("customer-sub", "customer1", false);
    private static final CurrentUser OTHER_CUSTOMER = new CurrentUser("other-sub", "customer2", false);

    @Autowired
    private IdempotentOrderService idempotentOrderService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;
    @MockitoBean
    private ErpGateway erpGateway;
    @MockitoBean
    private TaxGateway taxGateway;
    @MockitoBean
    private ClockGateway clockGateway;

    @BeforeEach
    void setUp() {
        idempotencyKeyRepository.deleteAll();
        orderRepository.deleteAll();
        couponRepository.deleteAll();

        when(clockGateway.getCurrentTime()).thenReturn(Instant.parse("2025-06-15T10:00:00Z"));
        var product = new ErpProductDetailsResponse();
        product.setPrice(new BigDecimal("10.00"));
        when(erpGateway.getProductDetails("BOOK-123")).thenReturn(Optional.of(product));
        var promotion = new ErpGetPromotionResponse();
        promotion.setPromotionActive(false);
        promotion.setDiscount(BigDecimal.ONE);
        when(erpGateway.getPromotionDetails()).thenReturn(promotion);
        var tax = new TaxDetailsResponse();
        tax.setTaxRate(new BigDecimal("0.10"));
        when(taxGateway.getTaxDetails("US")).thenReturn(Optional.of(tax));
    }

    private PlaceOrderRequest request(int quantity, String couponCode) {
        var request = new PlaceOrderRequest();
        request.setSku("BOOK-123");
        request.setQuantity(quantity);
        request.setCountry("US");
        request.setCouponCode(couponCode);
        return request;
    }

    @Test
    void concurrentFirstUsesOfOneKeyCreateExactlyOneOrder() throws Exception {
        couponService.createCoupon("SAVE10", new BigDecimal("0.1000"), null, null, 100);
        int threads = 8;
        var executor = Executors.newFixedThreadPool(threads);
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<String>>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                return idempotentOrderService.placeOrder(request(1, "SAVE10"), CUSTOMER, "race-key").body().getOrderNumber();
            }));
        }
        start.countDown();
        var orderNumbers = new HashSet<String>();
        for (var future : futures) {
            orderNumbers.add(future.get());
        }
        executor.shutdown();

        assertThat(orderNumbers).hasSize(1);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1);
        assertThat(couponRepository.findByCode("SAVE10").orElseThrow().getUsedCount()).isEqualTo(1);
    }

    @Test
    void replayStoresNothingNewAndUsesTheCouponOnce() {
        couponService.createCoupon("SAVE10", new BigDecimal("0.1000"), null, null, 100);

        var first = idempotentOrderService.placeOrder(request(1, "SAVE10"), CUSTOMER, "key-1");
        var replay = idempotentOrderService.placeOrder(request(1, "SAVE10"), CUSTOMER, "key-1");

        assertThat(replay.body().getOrderNumber()).isEqualTo(first.body().getOrderNumber());
        assertThat(replay.status()).isEqualTo(201);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(couponRepository.findByCode("SAVE10").orElseThrow().getUsedCount()).isEqualTo(1);
    }

    @Test
    void sameKeyWithADifferentRequestIsRejectedAndPlacesNothing() {
        idempotentOrderService.placeOrder(request(1, null), CUSTOMER, "key-1");

        var thrown = catchThrowableOfType(ValidationException.class,
                () -> idempotentOrderService.placeOrder(request(2, null), CUSTOMER, "key-1"));

        assertThat(thrown).hasMessage("Idempotency-Key has already been used with a different request");
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void keysAreScopedPerUser() {
        var mine = idempotentOrderService.placeOrder(request(1, null), CUSTOMER, "shared-key");
        var theirs = idempotentOrderService.placeOrder(request(1, null), OTHER_CUSTOMER, "shared-key");

        assertThat(theirs.body().getOrderNumber()).isNotEqualTo(mine.body().getOrderNumber());
        assertThat(orderRepository.count()).isEqualTo(2);
    }

    @Test
    void failedRequestLeavesTheKeyFreeForACorrectedRetry() {
        when(taxGateway.getTaxDetails("XX")).thenReturn(Optional.empty());
        var bad = request(1, null);
        bad.setCountry("XX");

        catchThrowableOfType(ValidationException.class, () -> idempotentOrderService.placeOrder(bad, CUSTOMER, "key-1"));

        assertThat(idempotencyKeyRepository.count()).isZero();
        var retried = idempotentOrderService.placeOrder(request(1, null), CUSTOMER, "key-1");
        assertThat(retried.body().getOrderNumber()).startsWith("ORD-");
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void expiredKeyIsReplacedOnItsNextUse() {
        idempotencyKeyRepository.save(new IdempotencyKey("customer-sub", "old-key", "stale-hash", 201,
                "{\"orderNumber\":\"ORD-OLD\"}", Instant.now().minus(IdempotencyKeyStore.TTL).minus(Duration.ofMinutes(1))));

        var result = idempotentOrderService.placeOrder(request(1, null), CUSTOMER, "old-key");

        assertThat(result.body().getOrderNumber()).isNotEqualTo("ORD-OLD");
        assertThat(orderRepository.count()).isEqualTo(1);
        var row = idempotencyKeyRepository.findByOwnerAndKey("customer-sub", "old-key").orElseThrow();
        assertThat(row.getResponseBody()).contains(result.body().getOrderNumber());
    }
}
