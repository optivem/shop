package com.mycompany.myshop.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.mycompany.myshop.backend.backendtest.configuration.TestcontainersConfiguration;
import com.mycompany.myshop.backend.core.dtos.CurrentUser;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.core.dtos.external.ErpGetPromotionResponse;
import com.mycompany.myshop.backend.core.dtos.external.ErpProductDetailsResponse;
import com.mycompany.myshop.backend.core.dtos.external.TaxDetailsResponse;
import com.mycompany.myshop.backend.core.entities.Order;
import com.mycompany.myshop.backend.core.entities.OrderStatus;
import com.mycompany.myshop.backend.core.exceptions.ValidationException;
import com.mycompany.myshop.backend.core.repositories.CouponRepository;
import com.mycompany.myshop.backend.core.repositories.OrderRepository;
import com.mycompany.myshop.backend.core.services.CouponService;
import com.mycompany.myshop.backend.core.services.OrderService;
import com.mycompany.myshop.backend.core.services.external.ClockGateway;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import com.mycompany.myshop.backend.core.services.external.TaxGateway;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OrderConsistencyIntegrationTest {

    private static final CurrentUser CUSTOMER = new CurrentUser("customer-sub", "customer1", false);

    @Autowired
    private OrderService orderService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponRepository couponRepository;
    @MockitoSpyBean
    private OrderRepository orderRepository;
    @MockitoBean
    private ErpGateway erpGateway;
    @MockitoBean
    private TaxGateway taxGateway;
    @MockitoBean
    private ClockGateway clockGateway;

    @BeforeEach
    void setUp() {
        reset(orderRepository);
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

    private PlaceOrderRequest request(String couponCode) {
        var request = new PlaceOrderRequest();
        request.setSku("BOOK-123");
        request.setQuantity(1);
        request.setCountry("US");
        request.setCouponCode(couponCode);
        return request;
    }

    @Test
    void failureWhileSavingOrderRollsBackCouponIncrement() {
        couponService.createCoupon("SAVE10", new BigDecimal("0.1000"), null, null, 5);
        doThrow(new IllegalStateException("boom")).when(orderRepository).save(any(Order.class));

        assertThatThrownBy(() -> orderService.placeOrder(request("SAVE10"), CUSTOMER))
                .isInstanceOf(RuntimeException.class);

        assertThat(orderRepository.count()).isZero();
        assertThat(couponRepository.findByCode("SAVE10").orElseThrow().getUsedCount()).isZero();
    }

    @Test
    void concurrentOrdersNeverExceedCouponUsageLimit() throws Exception {
        couponService.createCoupon("ONCE", new BigDecimal("0.1000"), null, null, 1);
        int threads = 8;
        var executor = Executors.newFixedThreadPool(threads);
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<Boolean>>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                try {
                    orderService.placeOrder(request("ONCE"), CUSTOMER);
                    return true;
                } catch (ValidationException e) {
                    return false;
                }
            }));
        }
        start.countDown();
        int successes = 0;
        for (var future : futures) {
            if (future.get()) {
                successes++;
            }
        }
        executor.shutdown();

        assertThat(successes).isEqualTo(1);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(couponRepository.findByCode("ONCE").orElseThrow().getUsedCount()).isEqualTo(1);
    }

    @Test
    void concurrentUpdatesToSameOrderConflict() {
        var number = orderService.placeOrder(request(null), CUSTOMER).getOrderNumber();
        var first = orderRepository.findByOrderNumber(number).orElseThrow();
        var second = orderRepository.findByOrderNumber(number).orElseThrow();

        first.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(first);
        second.setStatus(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> orderRepository.save(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(orderRepository.findByOrderNumber(number).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.DELIVERED);
    }
}
