package com.mycompany.myshop.backend.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycompany.myshop.backend.api.controller.AdminController;
import com.mycompany.myshop.backend.api.controller.CouponController;
import com.mycompany.myshop.backend.api.controller.HealthController;
import com.mycompany.myshop.backend.api.controller.OrderController;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.backend.core.services.CouponService;
import com.mycompany.myshop.backend.core.services.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest({OrderController.class, CouponController.class, AdminController.class, HealthController.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "allowed.origins=http://app.test")
@Import({SecurityConfig.class, ProblemDetailSecurityHandlers.class, CorsConfig.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CouponService couponService;

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor customer() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test
    void ordersWithoutTokenReturnsUnauthorizedProblemDetail() throws Exception {
        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void customerCanBrowseOrders() throws Exception {
        mockMvc.perform(get("/api/orders").with(customer())).andExpect(status().isOk());
    }

    @Test
    void customerCannotBrowseCoupons() throws Exception {
        mockMvc.perform(get("/api/coupons").with(customer()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void customerCannotDeliverOrder() throws Exception {
        mockMvc.perform(post("/api/orders/ORD-1/deliver").with(customer()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void customerCannotPublishCoupon() throws Exception {
        mockMvc.perform(post("/api/coupons").with(customer())
                .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotUseAdminEndpoints() throws Exception {
        mockMvc.perform(post("/api/admin/recall/BOOK-123").with(customer()))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotPlaceOrder() throws Exception {
        mockMvc.perform(post("/api/orders").with(admin())
                .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void customerCanPlaceOrder() throws Exception {
        var response = new PlaceOrderResponse();
        response.setOrderNumber("ORD-1");
        when(orderService.placeOrder(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/orders").with(customer())
                .contentType("application/json")
                .content("{\"sku\":\"BOOK-123\",\"quantity\":1,\"country\":\"US\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void adminCanBrowseAllOrders() throws Exception {
        mockMvc.perform(get("/api/orders").with(admin())).andExpect(status().isOk());
    }

    @Test
    void adminCanDeliverOrder() throws Exception {
        mockMvc.perform(post("/api/orders/ORD-1/deliver").with(admin()))
            .andExpect(status().isNoContent());
    }

    @Test
    void adminCanUseAdminEndpoints() throws Exception {
        mockMvc.perform(post("/api/admin/recall/BOOK-123").with(admin()))
            .andExpect(status().isOk());
    }

    @Test
    void unknownPathRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/unknown")).andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightPassesWithoutToken() throws Exception {
        mockMvc.perform(options("/api/orders")
                .header("Origin", "http://app.test")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://app.test"));
    }
}
