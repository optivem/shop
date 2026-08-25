package com.mycompany.myshop.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycompany.myshop.backend.domain.gateways.ErpGatewayException;
import com.mycompany.myshop.backend.presentation.CursorCodec;
import com.mycompany.myshop.backend.presentation.UseCaseResponder;
import com.mycompany.myshop.backend.presentation.controller.OrderController;
import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistoryRequest;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.usecases.order.CancelOrderRequest;
import com.mycompany.myshop.backend.usecases.order.DeliverOrderRequest;
import com.mycompany.myshop.backend.usecases.order.PlaceOrderResponse;
import com.mycompany.myshop.backend.usecases.order.ViewOrderDetailsRequest;
import com.mycompany.myshop.backend.usecases.order.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistory;
import com.mycompany.myshop.backend.usecases.order.CancelOrder;
import com.mycompany.myshop.backend.usecases.order.DeliverOrder;
import com.mycompany.myshop.backend.usecases.order.PlaceOrder;
import com.mycompany.myshop.backend.usecases.order.ViewOrderDetails;
import com.mycompany.myshop.backend.usecases.queries.OrderCursor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// No ERP stub -> no DSL refactor -> no latest/legacy twin. This test stubs the use cases with
// Mockito, an internal seam, not an external system.
//
// The subject is the presentation adapter: routing, status codes, the Location header, request
// validation and the mapping of use case outcomes onto HTTP. In backend-java the one collaborator to
// mock was OrderService; here it is five use case classes, which is the visible shape of the
// refactor at this boundary -- and mocking them one by one is what pins that the controller does
// nothing but delegate. The response DTOs it serializes still carry BigDecimal, unchanged: the wire
// is a fixed point of the refactor.
//
// UseCaseResponder is imported rather than mocked: it is not a collaborator the controller decides
// anything with, it *is* the failure half of the wire contract under test. @WebMvcTest only scans
// controller-ish beans, so a plain @Component needs saying explicitly.
@WebMvcTest(OrderController.class)
@Import({UseCaseResponder.class, CursorCodec.class})
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrowseOrderHistory browseOrderHistory;

    @MockitoBean
    private PlaceOrder placeOrder;

    @MockitoBean
    private ViewOrderDetails viewOrderDetails;

    @MockitoBean
    private CancelOrder cancelOrder;

    @MockitoBean
    private DeliverOrder deliverOrder;

    @Test
    void placeOrderReturnsCreated() throws Exception {
        var response = new PlaceOrderResponse();
        response.setOrderNumber("ORD-001");
        when(placeOrder.execute(any())).thenReturn(Result.ok(response));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"BOOK-123\",\"quantity\":2,\"country\":\"US\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/orders/ORD-001"))
            .andExpect(jsonPath("$.orderNumber").value("ORD-001"));
    }

    // The other half of the gateway contract. ErpGatewayIntegrationTest pins that the adapter throws
    // this when the ERP fails it; this pins what the caller is then told: 502, not the catch-all 500,
    // and a body that repeats none of the exception's message -- which in production names the
    // upstream URL and whatever it sent back.
    @Test
    void placeOrderReturnsBadGatewayWhenAnExternalSystemDoesNotAnswer() throws Exception {
        when(placeOrder.execute(any()))
            .thenThrow(new ErpGatewayException("ERP API returned status 503 for SKU: BOOK-123. "
                    + "URL: http://erp.internal:9001/api/products/BOOK-123"));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"BOOK-123\",\"quantity\":2,\"country\":\"US\"}"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.detail").value("An external system did not answer. Please try again later."));
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
        when(browseOrderHistory.execute(new BrowseOrderHistoryRequest(null, null, null)))
            .thenReturn(Result.ok(response));

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasMore").value(false))
            .andExpect(jsonPath("$.nextCursor").isEmpty());
    }

    // The cursor reaching the wire as one opaque string is the contract, so the expected token is
    // spelled out rather than re-derived with the codec: a test that encodes the value it asserts on
    // would pass whatever the format silently became.
    @Test
    void browseOrderHistoryEncodesTheNextCursor() throws Exception {
        var response = new BrowseOrderHistoryResponse();
        response.setOrders(List.of());
        response.setHasMore(true);
        response.setNextCursor(new OrderCursor(Instant.parse("2026-03-10T12:00:00Z"), "ORD-001"));
        when(browseOrderHistory.execute(any(BrowseOrderHistoryRequest.class)))
            .thenReturn(Result.ok(response));

        mockMvc.perform(get("/api/orders").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasMore").value(true))
            .andExpect(jsonPath("$.nextCursor").value("MjAyNi0wMy0xMFQxMjowMDowMFp8T1JELTAwMQ"));
    }

    @Test
    void browseOrderHistoryDecodesTheCursorItHandedOut() throws Exception {
        var response = new BrowseOrderHistoryResponse();
        response.setOrders(List.of());
        when(browseOrderHistory.execute(
                new BrowseOrderHistoryRequest(
                    null, null, new OrderCursor(Instant.parse("2026-03-10T12:00:00Z"), "ORD-001"))))
            .thenReturn(Result.ok(response));

        mockMvc.perform(get("/api/orders")
                .param("cursor", "MjAyNi0wMy0xMFQxMjowMDowMFp8T1JELTAwMQ"))
            .andExpect(status().isOk());
    }

    // 400 rather than 422: a cursor is not a field the caller filled in wrong, it is a token the
    // caller was never meant to author.
    @Test
    void browseOrderHistoryRejectsAMalformedCursor() throws Exception {
        mockMvc.perform(get("/api/orders").param("cursor", "not a cursor"))
            .andExpect(status().isBadRequest());
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
        response.setStatus("PLACED");
        response.setCountry("US");
        when(viewOrderDetails.execute(new ViewOrderDetailsRequest("ORD-001"))).thenReturn(Result.ok(response));

        mockMvc.perform(get("/api/orders/ORD-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNumber").value("ORD-001"));
    }

    @Test
    void getOrderNotFoundReturnsNotFound() throws Exception {
        when(viewOrderDetails.execute(new ViewOrderDetailsRequest("UNKNOWN")))
            .thenReturn(Result.err(new UseCaseError.NotFound("Order", "UNKNOWN")));

        mockMvc.perform(get("/api/orders/UNKNOWN"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Order UNKNOWN does not exist."));
    }

    @Test
    void getOrderInvalidReturnsUnprocessableEntityWithFieldErrors() throws Exception {
        when(viewOrderDetails.execute(new ViewOrderDetailsRequest("ORD-001")))
            .thenReturn(Result.err(new UseCaseError.Invalid("orderNumber", "Order number must not be empty")));

        mockMvc.perform(get("/api/orders/ORD-001"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errors[0].field").value("orderNumber"))
            .andExpect(jsonPath("$.errors[0].message").value("Order number must not be empty"));
    }

    @Test
    void cancelOrderReturnsNoContent() throws Exception {
        when(cancelOrder.execute(new CancelOrderRequest("ORD-001"))).thenReturn(Result.ok());

        mockMvc.perform(post("/api/orders/ORD-001/cancel"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deliverOrderReturnsNoContent() throws Exception {
        when(deliverOrder.execute(new DeliverOrderRequest("ORD-001"))).thenReturn(Result.ok());

        mockMvc.perform(post("/api/orders/ORD-001/deliver"))
            .andExpect(status().isNoContent());
    }
}
