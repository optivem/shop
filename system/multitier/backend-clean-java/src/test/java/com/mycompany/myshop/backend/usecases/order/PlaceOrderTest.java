package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.domain.entities.Product;
import com.mycompany.myshop.backend.domain.values.Promotion;
import com.mycompany.myshop.backend.domain.entities.TaxRate;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.usecases.TransactionRunner;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceOrderTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private ErpGateway erpGateway;
    @Mock
    private TaxGateway taxGateway;
    @Mock
    private ClockGateway clockGateway;
    @Mock
    private TransactionRunner transactionRunner;

    @InjectMocks
    private PlaceOrder placeOrder;

    private static final Instant NORMAL_TIME = Instant.parse("2025-06-15T10:00:00Z");
    private static final Instant DEC_31_YEAR_END_BLACKOUT = Instant.parse("2025-12-31T23:59:00Z");

    @BeforeEach
    void runTransactionsInline() {
        when(transactionRunner.inTransaction(any()))
                .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
    }

    @Test
    void placeOrderReturnsOrderNumberStartingWithOrd() {
        givenNormalTime();
        givenProductExists("BOOK-123", Money.of("10.00"));
        givenNoPromotion();
        givenTaxRate("US", Rate.of("0.10"));

        var result = placeOrder.execute(buildRequest("BOOK-123", 2, "US"));

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertSavedOrder(captor.getValue(), result.value());
    }

    @Test
    void placeOrderReportsInvalidWhenOrderedOnYearEndBlackout() {
        when(clockGateway.getCurrentTime()).thenReturn(DEC_31_YEAR_END_BLACKOUT);

        var result = placeOrder.execute(buildRequest("BOOK-123", 1, "US"));

        assertThat(result.error()).isInstanceOfSatisfying(UseCaseError.Invalid.class,
                invalid -> assertThat(invalid.message()).contains("December 31"));
    }

    @Test
    void placeOrderReportsInvalidWhenSkuUnknown() {
        givenNormalTime();
        when(erpGateway.getProductDetails("UNKNOWN")).thenReturn(Optional.empty());

        var result = placeOrder.execute(buildRequest("UNKNOWN", 1, "US"));

        assertThat(result.error()).isInstanceOfSatisfying(UseCaseError.Invalid.class,
                invalid -> assertThat(invalid.field()).isEqualTo("sku"));
    }

    @Test
    void placeOrderReportsInvalidWhenCountryUnknown() {
        givenNormalTime();
        givenProductExists("BOOK-123", Money.of("10.00"));
        givenNoPromotion();
        when(taxGateway.getTaxDetails(Country.of("XX"))).thenReturn(Optional.empty());

        var result = placeOrder.execute(buildRequest("BOOK-123", 1, "XX"));

        assertThat(result.error()).isInstanceOfSatisfying(UseCaseError.Invalid.class,
                invalid -> assertThat(invalid.field()).isEqualTo("country"));
    }

    private void givenNormalTime() {
        when(clockGateway.getCurrentTime()).thenReturn(NORMAL_TIME);
    }

    private void givenProductExists(String sku, Money price) {
        when(erpGateway.getProductDetails(sku)).thenReturn(Optional.of(new Product(sku, price)));
    }

    private void givenNoPromotion() {
        when(erpGateway.getPromotionDetails()).thenReturn(Promotion.inactive());
    }

    private void givenTaxRate(String country, Rate rate) {
        when(taxGateway.getTaxDetails(Country.of(country)))
                .thenReturn(Optional.of(new TaxRate(country, Country.of(country), rate)));
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
        assertThat(saved.getCountry()).isEqualTo(Country.of("US"));
        assertThat(saved.getUnitPrice()).isEqualTo(Money.of("10.00"));
        assertThat(saved.getBasePrice()).isEqualTo(Money.of("20.00"));
        assertThat(saved.getDiscountRate()).isEqualTo(Rate.ZERO);
        assertThat(saved.getDiscountAmount()).isEqualTo(Money.ZERO);
        assertThat(saved.getSubtotalPrice()).isEqualTo(Money.of("20.00"));
        assertThat(saved.getTaxRate()).isEqualTo(Rate.of("0.10"));
        assertThat(saved.getTaxAmount()).isEqualTo(Money.of("2.00"));
        assertThat(saved.getTotalPrice()).isEqualTo(Money.of("22.00"));
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(saved.getAppliedCouponCode()).isNull();
    }
}
