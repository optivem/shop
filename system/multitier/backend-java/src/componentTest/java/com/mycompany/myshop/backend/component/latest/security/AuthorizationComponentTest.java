package com.mycompany.myshop.backend.component.latest.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.backendtest.configuration.TestAuthConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthorizationComponentTest extends BaseComponentTest {

    private final TestRestTemplate client = new TestRestTemplate();

    private ResponseEntity<String> call(HttpMethod method, String path, String token) {
        var headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return client.exchange("http://localhost:" + port + path, method, new HttpEntity<>(headers), String.class);
    }

    @Test
    void healthIsPublic() {
        assertThat(call(HttpMethod.GET, "/health", null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void ordersWithoutTokenReturnsUnauthorized() {
        var response = call(HttpMethod.GET, "/api/orders", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("\"status\":401");
    }

    @Test
    void ordersWithInvalidTokenReturnsUnauthorized() {
        assertThat(call(HttpMethod.GET, "/api/orders", "garbage").getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void customerCanBrowseOrders() {
        assertThat(call(HttpMethod.GET, "/api/orders", TestAuthConfiguration.CUSTOMER_TOKEN).getStatusCode())
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    void customerCannotDeliverOrder() {
        var response = call(HttpMethod.POST, "/api/orders/ORD-1/deliver", TestAuthConfiguration.CUSTOMER_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("\"status\":403");
    }

    @Test
    void customerCannotPublishCoupon() {
        assertThat(call(HttpMethod.POST, "/api/coupons", TestAuthConfiguration.CUSTOMER_TOKEN).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void customerCannotUseAdminEndpoints() {
        assertThat(call(HttpMethod.POST, "/api/admin/recall/BOOK-123", TestAuthConfiguration.CUSTOMER_TOKEN)
            .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminPassesAuthorizationOnAdminEndpoint() {
        // The order does not exist, so the request is authorized and then rejected by the service.
        var response = call(HttpMethod.POST, "/api/orders/ORD-MISSING/deliver", TestAuthConfiguration.ADMIN_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
