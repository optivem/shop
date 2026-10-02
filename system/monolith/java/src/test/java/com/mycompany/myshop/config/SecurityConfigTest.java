package com.mycompany.myshop.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycompany.myshop.api.controller.CouponApiController;
import com.mycompany.myshop.api.controller.HealthController;
import com.mycompany.myshop.api.controller.OrderApiController;
import com.mycompany.myshop.controllers.web.AdminCouponsController;
import com.mycompany.myshop.controllers.web.HomeController;
import com.mycompany.myshop.core.services.CouponService;
import com.mycompany.myshop.core.services.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest({OrderApiController.class, CouponApiController.class, HealthController.class,
    HomeController.class, AdminCouponsController.class})
@ActiveProfiles("test")
@Import({SecurityConfig.class, ProblemDetailSecurityHandlers.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CouponService couponService;

    private static RequestPostProcessor adminToken() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor customerToken() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private static RequestPostProcessor adminSession() {
        return oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor customerSession() {
        return oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test
    void apiWithoutCredentialsReturnsUnauthorizedProblemDetail() throws Exception {
        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void apiUnknownPathRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/unknown")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerTokenCanBrowseOrders() throws Exception {
        mockMvc.perform(get("/api/orders").with(customerToken())).andExpect(status().isOk());
    }

    @Test
    void customerTokenCannotBrowseCoupons() throws Exception {
        mockMvc.perform(get("/api/coupons").with(customerToken()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void customerTokenCannotDeliverOrder() throws Exception {
        mockMvc.perform(post("/api/orders/ORD-1/deliver").with(customerToken()))
            .andExpect(status().isForbidden());
    }

    @Test
    void customerTokenCannotPublishCoupon() throws Exception {
        mockMvc.perform(post("/api/coupons").with(customerToken())
                .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminTokenCanDeliverOrder() throws Exception {
        mockMvc.perform(post("/api/orders/ORD-1/deliver").with(adminToken()))
            .andExpect(status().isNoContent());
    }

    @Test
    void homePageWithoutSessionRedirectsToKeycloakLogin() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/oauth2/authorization/keycloak"));
    }

    @Test
    void customerSessionCanOpenHomePage() throws Exception {
        mockMvc.perform(get("/").with(customerSession())).andExpect(status().isOk());
    }

    @Test
    void customerSessionCannotOpenAdminCouponsPage() throws Exception {
        mockMvc.perform(get("/admin-coupons").with(customerSession())).andExpect(status().isForbidden());
    }

    @Test
    void adminSessionCanOpenAdminCouponsPage() throws Exception {
        mockMvc.perform(get("/admin-coupons").with(adminSession())).andExpect(status().isOk());
    }

    @Test
    void sessionApiCallWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/orders/ORD-1/deliver").with(adminSession()))
            .andExpect(status().isForbidden());
    }

    @Test
    void sessionApiCallWithCsrfTokenIsAccepted() throws Exception {
        mockMvc.perform(post("/api/orders/ORD-1/deliver").with(adminSession()).with(csrf()))
            .andExpect(status().isNoContent());
    }
}
