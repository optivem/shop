package com.mycompany.myshop.systemtest.legacy.mod04.e2e;

import com.mycompany.myshop.systemtest.legacy.mod04.e2e.base.BaseE2eTest;
import com.mycompany.myshop.testkit.driver.adapter.api.client.ApiIdentity;
import com.mycompany.myshop.testkit.driver.port.dtos.PublishCouponRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.mycompany.myshop.testkit.common.ResultAssert.assertThatResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ApiAuthorizationTest extends BaseE2eTest {
    @Override
    protected void setMyShopClient() {
        setUpMyShopApiClient();
    }

    @BeforeEach
    void requireKeycloak() {
        var keycloakBaseUrl = configuration.getKeycloakBaseUrl();
        assumeTrue(keycloakBaseUrl != null && !keycloakBaseUrl.isBlank(),
                "Authorization tests require KEYCLOAK_URL to be set");
    }

    @Test
    void shouldNotRequireTokenForHealth() {
        var result = myShopApiClient.as(ApiIdentity.ANONYMOUS).health().checkHealth();

        assertThatResult(result).isSuccess();
    }

    @Test
    void shouldRejectRequestWithoutToken() {
        var result = myShopApiClient.as(ApiIdentity.ANONYMOUS).orders().viewOrder("ORD-NONEXISTENT");

        assertThatResult(result).isFailure();
        assertThat(result.getError().getStatus()).isEqualTo(401);
    }

    @Test
    void shouldRejectCustomerPublishingCoupon() {
        var result = myShopApiClient.as(ApiIdentity.CUSTOMER).coupons().publishCoupon(newCouponRequest());

        assertThatResult(result).isFailure();
        assertThat(result.getError().getStatus()).isEqualTo(403);
    }

    @Test
    void shouldAllowAdminToPublishCoupon() {
        var result = myShopApiClient.as(ApiIdentity.ADMIN).coupons().publishCoupon(newCouponRequest());

        assertThatResult(result).isSuccess();
    }

    private static PublishCouponRequest newCouponRequest() {
        return PublishCouponRequest.builder()
                .code("AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .discountRate("0.10")
                .build();
    }
}
