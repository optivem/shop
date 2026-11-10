package com.mycompany.myshop.controllers.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import com.mycompany.myshop.config.ProblemDetailSecurityHandlers;
import com.mycompany.myshop.config.SecurityConfig;
import com.mycompany.myshop.controllers.web.storefront.Cart;
import com.mycompany.myshop.controllers.web.storefront.CheckoutResult;
import com.mycompany.myshop.controllers.web.storefront.Notice;
import com.mycompany.myshop.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.core.dtos.ViewProductDetailsResponse;
import com.mycompany.myshop.core.dtos.ViewProfileResponse;
import com.mycompany.myshop.core.exceptions.ErpUnavailableException;
import com.mycompany.myshop.core.exceptions.NotExistValidationException;
import com.mycompany.myshop.core.exceptions.ValidationException;
import com.mycompany.myshop.core.services.CouponService;
import com.mycompany.myshop.core.services.IdempotencyKeyStore;
import com.mycompany.myshop.core.services.IdempotentOrderService;
import com.mycompany.myshop.core.services.OrderService;
import com.mycompany.myshop.core.services.ProductService;
import com.mycompany.myshop.core.services.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** The server-rendered storefront (storefront-ux §2–§4): pages, the session cart and checkout. */
@WebMvcTest
@ActiveProfiles("test")
@Import({SecurityConfig.class, ProblemDetailSecurityHandlers.class, IdempotentOrderService.class})
class StorefrontPagesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private IdempotencyKeyStore idempotencyKeyStore;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ProfileService profileService;

    private final MockHttpSession session = new MockHttpSession();

    @BeforeEach
    void catalog() {
        when(productService.browseProducts()).thenReturn(List.of(
                product("HP-15", "HP Laptop 15", "HP", "Laptops", "699.99"),
                product("DELL-XPS", "Dell XPS 13", "Dell", "Laptops", "1299.99"),
                product("LOGI-MX3S", "Logitech MX Master 3S", "Logitech", "Accessories", "99.99")));
    }

    private static RequestPostProcessor customer() {
        return oidcLogin().idToken(token -> token.claim("preferred_username", "customer"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private static RequestPostProcessor admin() {
        return oidcLogin().idToken(token -> token.claim("preferred_username", "admin"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    void anonymousVisitorBrowsesTheCatalogAndIsOfferedToSignIn() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Product DELL-XPS\"")))
                .andExpect(content().string(containsString("$1,299.99")))
                .andExpect(content().string(containsString("href=\"/sign-in?returnUrl=")))
                .andExpect(content().string(containsString("aria-label=\"Sign In\"")))
                .andExpect(content().string(not(containsString("href=\"/order-history\""))));
        mockMvc.perform(head("/")).andExpect(status().isOk());
    }

    @Test
    void signedInCustomerSeesEachAccountLinkOnceOnTheCatalog() throws Exception {
        var html = mockMvc.perform(get("/").with(customer())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("aria-label=\"Sign In\"").contains("data-testid=\"username\"");
        assertThat(count(html, "href=\"/order-history\"")).isEqualTo(1);
        assertThat(count(html, "href=\"/new-order\"")).isEqualTo(1);
        assertThat(html).doesNotContain("href=\"/admin-coupons\"");
    }

    @Test
    void signedInAdminSeesAllOrdersAndAdminButNoQuickOrder() throws Exception {
        var html = mockMvc.perform(get("/").with(admin())).andReturn().getResponse().getContentAsString();

        assertThat(count(html, "href=\"/order-history\"")).isEqualTo(1);
        assertThat(count(html, "href=\"/admin-coupons\"")).isEqualTo(1);
        assertThat(html).doesNotContain("href=\"/new-order\"").contains("All orders");
    }

    @Test
    void filterFormDefaultsRedirectToTheShortAddress() throws Exception {
        mockMvc.perform(get("/").param("q", "").param("category", "Laptops").param("sort", "featured"))
                .andExpect(redirectedUrl("/?category=Laptops"));
    }

    @Test
    void catalogTellsTheVisitorWhenTheErpIsUnavailable() throws Exception {
        when(productService.browseProducts()).thenThrow(new ErpUnavailableException("down"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Products are temporarily unavailable.")));
    }

    @Test
    void unknownSkuIsAProductNotFoundPage() throws Exception {
        when(productService.viewProduct("NO-SUCH-SKU")).thenThrow(new NotExistValidationException("missing"));

        mockMvc.perform(get("/products/NO-SUCH-SKU"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Product not found")));
    }

    @Test
    void addingToTheCartGoesBackWithANoticeAndRaisesTheBadge() throws Exception {
        var result = mockMvc.perform(post("/cart/add").session(session).with(csrf())
                        .param("sku", "HP-15").param("quantity", "2").param("returnUrl", "/products/HP-15"))
                .andExpect(redirectedUrl("/products/HP-15"))
                .andExpect(flash().attributeExists(Notice.ATTRIBUTE))
                .andReturn();
        assertThat(((Notice) result.getFlashMap().get(Notice.ATTRIBUTE)).message()).isEqualTo("Added to cart");

        mockMvc.perform(post("/cart/add").session(session).with(csrf()).param("sku", "HP-15"));

        mockMvc.perform(get("/cart").session(session))
                .andExpect(content().string(containsString("aria-label=\"Cart Line HP-15\"")))
                .andExpect(content().string(containsString("aria-label=\"Cart Item Count\"")))
                .andExpect(content().string(containsString("$2,099.97")));
        assertThat(Cart.of(session).totalQuantity()).isEqualTo(3);
    }

    @Test
    void anInvalidQuantityIsNotAdded() throws Exception {
        var result = mockMvc.perform(post("/cart/add").session(session).with(csrf())
                        .param("sku", "HP-15").param("quantity", "100"))
                .andReturn();

        assertThat(((Notice) result.getFlashMap().get(Notice.ATTRIBUTE)).message())
                .isEqualTo("Quantity must be a whole number from 1 to 99.");
        assertThat(Cart.of(session).isEmpty()).isTrue();
    }

    @Test
    void cartChangesNeedTheCsrfToken() throws Exception {
        mockMvc.perform(post("/cart/add").param("sku", "HP-15")).andExpect(status().isForbidden());
    }

    @Test
    void checkoutWithAnEmptyCartGoesToTheCart() throws Exception {
        mockMvc.perform(get("/checkout").session(session).with(customer())).andExpect(redirectedUrl("/cart"));
    }

    @Test
    void checkoutPreselectsTheSavedAddressAndEstimatesItsTax() throws Exception {
        when(profileService.getProfile(any())).thenReturn(savedProfile());
        Cart.of(session).add("HP-15", 1);

        mockMvc.perform(get("/checkout").session(session).with(customer()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ana Novak, Main St 1, 10115 Berlin, DE")))
                .andExpect(content().string(containsString(">19%<")));
    }

    @Test
    void checkoutPlacesOneOrderPerLineAndConfirmsThem() throws Exception {
        when(profileService.getProfile(any())).thenReturn(savedProfile());
        when(orderService.placeOrder(argThat(request -> request != null && "HP-15".equals(request.getSku())), any()))
                .thenReturn(placed("ORD-1"));
        when(orderService.placeOrder(argThat(request -> request != null && "DELL-XPS".equals(request.getSku())), any()))
                .thenThrow(new ValidationException("couponCode", "Coupon code LIMITED2024 has exceeded its usage limit"));
        Cart.of(session).add("HP-15", 2);
        Cart.of(session).add("DELL-XPS", 1);

        mockMvc.perform(post("/checkout").session(session).with(customer()).with(csrf())
                        .param("addressChoice", "saved").param("couponCode", "LIMITED2024"))
                .andExpect(redirectedUrl("/checkout/confirmation"));

        assertThat(Cart.of(session).lines()).containsExactly(new Cart.CartLine("DELL-XPS", 1));
        var result = (CheckoutResult) session.getAttribute(CheckoutResult.SESSION_ATTRIBUTE);
        assertThat(result.rejected()).extracting(CheckoutResult.RejectedLine::reason)
                .containsExactly("Coupon code LIMITED2024 has exceeded its usage limit");

        mockMvc.perform(get("/checkout/confirmation").session(session).with(customer()))
                .andExpect(content().string(containsString("Thank you for your order!")))
                .andExpect(content().string(containsString("aria-label=\"Confirmed Order ORD-1\"")))
                .andExpect(content().string(containsString("href=\"/order-details?orderNumber=ORD-1\"")));
    }

    @Test
    void checkoutPlacesNothingWhenEveryLineIsRejected() throws Exception {
        when(profileService.getProfile(any())).thenReturn(savedProfile());
        when(orderService.placeOrder(any(), any()))
                .thenThrow(new ValidationException("couponCode", "Coupon code INVALIDCOUPON does not exist"));
        Cart.of(session).add("HP-15", 1);

        mockMvc.perform(post("/checkout").session(session).with(customer()).with(csrf())
                        .param("addressChoice", "saved").param("couponCode", "INVALIDCOUPON"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No order was placed.")))
                .andExpect(content().string(containsString("HP-15: Coupon code INVALIDCOUPON does not exist")));

        assertThat(Cart.of(session).lines()).containsExactly(new Cart.CartLine("HP-15", 1));
    }

    @Test
    void checkoutRejectsAnIncompleteAddressWithoutPlacingAnOrder() throws Exception {
        when(profileService.getProfile(any())).thenReturn(new ViewProfileResponse());
        Cart.of(session).add("HP-15", 1);

        mockMvc.perform(post("/checkout").session(session).with(customer()).with(csrf())
                        .param("firstName", "Ana").param("lastName", "Novak").param("street", "Main St 1")
                        .param("postalCode", "10115").param("city", "").param("country", "DE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("City is required")));

        verify(orderService, never()).placeOrder(any(), any());
    }

    @Test
    void quickOrderCountryComesFromTheProfile() throws Exception {
        var profile = new ViewProfileResponse();
        profile.setCountry("GB");
        when(profileService.getProfile(any())).thenReturn(profile);

        mockMvc.perform(get("/new-order").with(customer()))
                .andExpect(content().string(containsString("value=\"GB\"")));
    }

    @Test
    void signInReturnsToTheRequestedPage() throws Exception {
        mockMvc.perform(get("/sign-in").param("returnUrl", "/cart").with(customer())).andExpect(redirectedUrl("/cart"));
        mockMvc.perform(get("/sign-in").param("returnUrl", "//evil.example").with(customer())).andExpect(redirectedUrl("/"));
    }

    private static ViewProfileResponse savedProfile() {
        var profile = new ViewProfileResponse();
        profile.setFirstName("Ana");
        profile.setLastName("Novak");
        profile.setStreet("Main St 1");
        profile.setPostalCode("10115");
        profile.setCity("Berlin");
        profile.setCountry("DE");
        return profile;
    }

    private static PlaceOrderResponse placed(String orderNumber) {
        var response = new PlaceOrderResponse();
        response.setOrderNumber(orderNumber);
        return response;
    }

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    private static ViewProductDetailsResponse product(String sku, String title, String brand, String category, String price) {
        var product = new ViewProductDetailsResponse();
        product.setSku(sku);
        product.setTitle(title);
        product.setBrand(brand);
        product.setCategory(category);
        product.setPrice(new BigDecimal(price));
        product.setDescription(title);
        return product;
    }
}
