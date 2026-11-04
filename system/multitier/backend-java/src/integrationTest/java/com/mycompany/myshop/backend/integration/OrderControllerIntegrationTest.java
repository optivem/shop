package com.mycompany.myshop.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycompany.myshop.backend.api.controller.OrderController;
import com.mycompany.myshop.backend.config.ProblemDetailSecurityHandlers;
import com.mycompany.myshop.backend.config.SecurityConfig;
import com.mycompany.myshop.backend.core.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.backend.core.dtos.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.core.entities.OrderStatus;
import com.mycompany.myshop.backend.core.exceptions.NotExistValidationException;
import com.mycompany.myshop.backend.core.services.IdempotencyKeyStore;
import com.mycompany.myshop.backend.core.services.IdempotentOrderService;
import com.mycompany.myshop.backend.core.services.OrderService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// No ERP stub -> no DSL refactor -> no latest/legacy twin. This test stubs OrderService with Mockito,
// an internal seam, not an external system.
@WebMvcTest(OrderController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, ProblemDetailSecurityHandlers.class, IdempotentOrderService.class,
    OrderControllerIntegrationTest.AdminRequests.class})
class OrderControllerIntegrationTest {

    /** Every request runs as a user holding both roles (to place and to administer); authorization has its own tests. */
    @TestConfiguration
    static class AdminRequests {
        @Bean
        MockMvcBuilderCustomizer adminByDefault() {
            return builder -> builder.defaultRequest(get("/").with(jwt().authorities(
                new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        }
    }

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    // IdempotentOrderService is real (imported), so POST /api/orders still reaches the OrderService
    // mock; its store is mocked and, unstubbed, finds no stored response.
    @MockitoBean
    private IdempotencyKeyStore idempotencyKeyStore;

    @Test
    void placeOrderReturnsCreated() throws Exception {
        var response = new PlaceOrderResponse();
        response.setOrderNumber("ORD-001");
        when(orderService.placeOrder(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"BOOK-123\",\"quantity\":2,\"country\":\"US\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/orders/ORD-001"))
            .andExpect(jsonPath("$.orderNumber").value("ORD-001"));
    }

    // A replay answers from the stored response: same status, same Location, same body, and the
    // order service is never asked to place a second order.
    @Test
    void placeOrderWithUsedIdempotencyKeyReplaysStoredResponse() throws Exception {
        var stored = new PlaceOrderResponse();
        stored.setOrderNumber("ORD-001");
        when(idempotencyKeyStore.findReplay(any(), eq(PlaceOrderResponse.class)))
            .thenReturn(Optional.of(new IdempotencyKeyStore.StoredResponse<>(201, stored)));

        mockMvc.perform(post("/api/orders")
                .header("Idempotency-Key", "key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"BOOK-123\",\"quantity\":2,\"country\":\"US\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/orders/ORD-001"))
            .andExpect(jsonPath("$.orderNumber").value("ORD-001"));

        verify(orderService, never()).placeOrder(any(), any(), any());
        verify(orderService, never()).placeOrder(any(), any());
    }

    @Test
    void placeOrderWithTooLongIdempotencyKeyReturnsUnprocessableEntity() throws Exception {
        mockMvc.perform(post("/api/orders")
                .header("Idempotency-Key", "k".repeat(256))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"BOOK-123\",\"quantity\":2,\"country\":\"US\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errors[0].field").value("Idempotency-Key"));
    }

    @Test
    void placeOrderMissingRequiredFieldsReturnsUnprocessableEntity() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void browseOrderHistoryReturnsOk() throws Exception {
        var response = new BrowseOrderHistoryResponse();
        response.setOrders(List.of());
        when(orderService.browseOrderHistory(any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk());
    }

    @Test
    void getOrderReturnsOk() throws Exception {
        var response = new ViewOrderDetailsResponse();
        response.setOrderNumber("ORD-001");
        response.setOrderTimestamp(Instant.parse("2026-03-10T12:00:00Z"));
        response.setSku("BOOK-123");
        response.setQuantity(2);
        response.setUnitPrice(new BigDecimal("10.00"));
        response.setBasePrice(new BigDecimal("20.00"));
        response.setDiscountRate(BigDecimal.ZERO);
        response.setDiscountAmount(BigDecimal.ZERO);
        response.setSubtotalPrice(new BigDecimal("20.00"));
        response.setTaxRate(new BigDecimal("0.10"));
        response.setTaxAmount(new BigDecimal("2.00"));
        response.setTotalPrice(new BigDecimal("22.00"));
        response.setStatus(OrderStatus.PLACED);
        response.setCountry("US");
        when(orderService.getOrder(eq("ORD-001"), any())).thenReturn(response);

        mockMvc.perform(get("/api/orders/ORD-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNumber").value("ORD-001"));
    }

    @Test
    void getOrderNotFoundReturnsNotFound() throws Exception {
        when(orderService.getOrder(eq("UNKNOWN"), any()))
            .thenThrow(new NotExistValidationException("Order UNKNOWN not found"));

        mockMvc.perform(get("/api/orders/UNKNOWN"))
            .andExpect(status().isNotFound());
    }

    @Test
    void cancelOrderReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/orders/ORD-001/cancel"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deliverOrderReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/orders/ORD-001/deliver"))
            .andExpect(status().isNoContent());
    }
}
