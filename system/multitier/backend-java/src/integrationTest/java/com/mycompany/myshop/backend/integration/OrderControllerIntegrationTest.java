package com.mycompany.myshop.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycompany.myshop.backend.api.controller.OrderController;
import com.mycompany.myshop.backend.core.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.backend.core.dtos.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.core.entities.OrderStatus;
import com.mycompany.myshop.backend.core.exceptions.NotExistValidationException;
import com.mycompany.myshop.backend.core.services.OrderService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// No ERP stub -> no DSL refactor -> no latest/legacy twin. This test stubs OrderService with Mockito,
// an internal seam, not an external system.
@WebMvcTest(OrderController.class)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void placeOrderReturnsCreated() throws Exception {
        var response = new PlaceOrderResponse();
        response.setOrderNumber("ORD-001");
        when(orderService.placeOrder(any())).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"BOOK-123\",\"quantity\":2,\"country\":\"US\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/orders/ORD-001"))
            .andExpect(jsonPath("$.orderNumber").value("ORD-001"));
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
        when(orderService.browseOrderHistory(null, null, null)).thenReturn(response);

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
        when(orderService.getOrder("ORD-001")).thenReturn(response);

        mockMvc.perform(get("/api/orders/ORD-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNumber").value("ORD-001"));
    }

    @Test
    void getOrderNotFoundReturnsNotFound() throws Exception {
        when(orderService.getOrder("UNKNOWN"))
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
