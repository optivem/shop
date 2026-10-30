package com.mycompany.myshop.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.mycompany.myshop.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.core.services.CouponService;
import com.mycompany.myshop.core.services.OrderService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

// No controller list: a new controller must load here so the route guard below can see it.
@WebMvcTest
@ActiveProfiles("test")
@Import({SecurityConfig.class, ProblemDetailSecurityHandlers.class})
class SecurityConfigTest {

    private static final String APPLICATION_PACKAGE = "com.mycompany.myshop";
    /** The default timestamp validator tolerates 60 seconds of clock skew. */
    private static final Duration CLOCK_SKEW_EXCEEDED = Duration.ofMinutes(5);
    private static final RSAKey SIGNING_KEY = generateSigningKey();

    /** Every controller route. Enforced in both directions by everyControllerRouteHasAnExpectedAccessRule. */
    private static final Map<Route, Access> EXPECTED_ROUTES = Map.ofEntries(
        route("GET", "/health", Access.PUBLIC),
        route("GET", "/api/orders", Access.AUTHENTICATED),
        route("POST", "/api/orders", Access.CUSTOMER_ONLY),
        route("GET", "/api/orders/{orderNumber}", Access.AUTHENTICATED),
        route("POST", "/api/orders/{orderNumber}/cancel", Access.AUTHENTICATED),
        route("POST", "/api/orders/{orderNumber}/deliver", Access.ADMIN_ONLY),
        route("GET", "/api/coupons", Access.ADMIN_ONLY),
        route("POST", "/api/coupons", Access.ADMIN_ONLY),
        route("GET", "/", Access.AUTHENTICATED),
        route("GET", "/new-order", Access.CUSTOMER_ONLY),
        route("POST", "/new-order", Access.CUSTOMER_ONLY),
        route("GET", "/order-history", Access.AUTHENTICATED),
        route("GET", "/order-details/{orderNumber}", Access.AUTHENTICATED),
        route("GET", "/admin-coupons", Access.ADMIN_ONLY));

    private static HttpServer jwksServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Value("${auth.issuer-uri}")
    private String issuerUri;

    @Value("${auth.audience}")
    private String audience;

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

    /** Serves the signing key so the real {@code jwtDecoder()} bean validates tokens end to end. */
    @DynamicPropertySource
    static void serveJwks(DynamicPropertyRegistry registry) throws IOException {
        byte[] jwks = new JWKSet(SIGNING_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        jwksServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        jwksServer.createContext("/certs", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            try (var body = exchange.getResponseBody()) {
                body.write(jwks);
            }
        });
        jwksServer.start();
        registry.add("auth.jwk-set-uri", () -> "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/certs");
    }

    @AfterAll
    static void stopJwksServer() {
        jwksServer.stop(0);
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
    void adminTokenCannotPlaceOrder() throws Exception {
        mockMvc.perform(post("/api/orders").with(adminToken())
                .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value("You do not have permission to perform this action"));
    }

    @Test
    void anonymousCannotPlaceOrder() throws Exception {
        mockMvc.perform(post("/api/orders").with(csrf())
                .contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Authentication is required"));
    }

    @Test
    void customerTokenCanPlaceOrder() throws Exception {
        var response = new PlaceOrderResponse();
        response.setOrderNumber("ORD-1");
        when(orderService.placeOrder(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/orders").with(customerToken())
                .contentType("application/json")
                .content("{\"sku\":\"BOOK-123\",\"quantity\":1,\"country\":\"US\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void adminTokenCanBrowseAllOrders() throws Exception {
        mockMvc.perform(get("/api/orders").with(adminToken())).andExpect(status().isOk());
    }

    @Test
    void adminSessionCannotOpenNewOrderPage() throws Exception {
        mockMvc.perform(get("/new-order").with(adminSession())).andExpect(status().isForbidden());
    }

    @Test
    void customerSessionCanOpenNewOrderPage() throws Exception {
        mockMvc.perform(get("/new-order").with(customerSession())).andExpect(status().isOk());
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
    void homePageWithoutSessionAndWithoutAcceptHeaderRedirectsToKeycloakLogin() throws Exception {
        mockMvc.perform(get("/"))
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

    @Test
    void everyControllerRouteHasAnExpectedAccessRule() {
        var mapped = handlerMapping.getHandlerMethods().entrySet().stream()
            .filter(entry -> entry.getValue().getBeanType().getName().startsWith(APPLICATION_PACKAGE))
            .flatMap(entry -> routesOf(entry.getKey()))
            .toList();

        assertThat(mapped)
            .as("Add every new route to EXPECTED_ROUTES and remove every deleted one")
            .containsExactlyInAnyOrderElementsOf(EXPECTED_ROUTES.keySet());
    }

    @ParameterizedTest
    @MethodSource("expectedRoutes")
    void anonymousAccessMatchesExpectedRule(Route route, Access access) throws Exception {
        var result = mockMvc.perform(route.request().with(csrf())).andReturn();

        if (access.anonymous) {
            assertReachedController(result);
        } else {
            assertAuthenticationRequired(route, result);
        }
    }

    @ParameterizedTest
    @MethodSource("expectedRoutes")
    void customerAccessMatchesExpectedRule(Route route, Access access) throws Exception {
        assertRoleAccess(mockMvc.perform(route.request().with(csrf()).with(credentials(route, customerToken(), customerSession()))).andReturn(), access.customer);
    }

    @ParameterizedTest
    @MethodSource("expectedRoutes")
    void adminAccessMatchesExpectedRule(Route route, Access access) throws Exception {
        assertRoleAccess(mockMvc.perform(route.request().with(csrf()).with(credentials(route, adminToken(), adminSession()))).andReturn(), access.admin);
    }

    @Test
    void validBearerTokenIsAccepted() throws Exception {
        mockMvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, bearer(audience, Instant.now().plus(Duration.ofHours(1)))))
            .andExpect(status().isOk());
    }

    @Test
    void expiredBearerTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, bearer(audience, Instant.now().minus(CLOCK_SKEW_EXCEEDED))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void wrongAudienceBearerTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, bearer("another-api", Instant.now().plus(Duration.ofHours(1)))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    private static Stream<Arguments> expectedRoutes() {
        return EXPECTED_ROUTES.entrySet().stream().map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
    }

    private static Map.Entry<Route, Access> route(String method, String path, Access access) {
        return Map.entry(new Route(method, path), access);
    }

    /** A mapping without HTTP methods answers all of them; "ANY" keeps it visible to the guard. */
    private static Stream<Route> routesOf(RequestMappingInfo mapping) {
        var methods = mapping.getMethodsCondition().getMethods().stream().map(Enum::name).toList();
        return mapping.getPatternValues().stream()
            .flatMap(path -> (methods.isEmpty() ? List.of("ANY") : methods).stream()
                .map(method -> new Route(method, path)));
    }

    private static void assertRoleAccess(MvcResult result, boolean allowed) {
        if (allowed) {
            assertReachedController(result);
        } else {
            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(result.getHandler()).isNull();
        }
    }

    /** Rejections happen in the filter chain, so reaching a handler proves the security rules let it through. */
    private static void assertReachedController(MvcResult result) {
        assertThat(result.getHandler()).isInstanceOf(HandlerMethod.class);
    }

    /** API callers get a 401 problem detail; pages redirect to the Keycloak login by design. */
    private static void assertAuthenticationRequired(Route route, MvcResult result) {
        var response = result.getResponse();
        if (route.isApi()) {
            assertThat(response.getStatus()).isEqualTo(401);
        } else {
            assertThat(response.getStatus()).isEqualTo(302);
            assertThat(response.getRedirectedUrl()).endsWith("/oauth2/authorization/keycloak");
        }
        assertThat(result.getHandler()).isNull();
    }

    /** API routes are exercised with a Bearer token, pages with the browser session they really use. */
    private static RequestPostProcessor credentials(Route route, RequestPostProcessor token,
            RequestPostProcessor session) {
        return route.isApi() ? token : session;
    }

    private String bearer(String tokenAudience, Instant expiresAt) throws JOSEException {
        var claims = new JWTClaimsSet.Builder()
            .issuer(issuerUri)
            .audience(tokenAudience)
            .subject("customer-user")
            .issueTime(Date.from(expiresAt.minus(Duration.ofHours(2))))
            .expirationTime(Date.from(expiresAt))
            .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))
            .build();
        var token = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(SIGNING_KEY.getKeyID()).build(), claims);
        token.sign(new RSASSASigner(SIGNING_KEY));
        return "Bearer " + token.serialize();
    }

    private static RSAKey generateSigningKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("test-key").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Who a route admits. Ownership of a specific order is data-dependent and lives in the service layer. */
    enum Access {
        PUBLIC(true, true, true),
        AUTHENTICATED(false, true, true),
        CUSTOMER_ONLY(false, true, false),
        ADMIN_ONLY(false, false, true);

        private final boolean anonymous;
        private final boolean customer;
        private final boolean admin;

        Access(boolean anonymous, boolean customer, boolean admin) {
            this.anonymous = anonymous;
            this.customer = customer;
            this.admin = admin;
        }
    }

    record Route(String method, String path) {

        MockHttpServletRequestBuilder request() {
            return MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path.replaceAll("\\{[^}]+}", "1"));
        }

        boolean isApi() {
            return path.startsWith("/api/");
        }

        @Override
        public String toString() {
            return method + " " + path;
        }
    }
}
