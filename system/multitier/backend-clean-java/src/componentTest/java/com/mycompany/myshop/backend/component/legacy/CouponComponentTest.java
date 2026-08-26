package com.mycompany.myshop.backend.component.legacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.usecases.queries.coupon.BrowseCouponsItemResponse;
import com.mycompany.myshop.backend.usecases.queries.coupon.BrowseCouponsResponse;
import com.mycompany.myshop.backend.usecases.commands.coupon.PublishCouponRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CouponComponentTest extends BaseComponentTest {

    @Test
    void publishReturnsNoContentThenBrowseListsCoupon() {
        var request = new PublishCouponRequest();
        request.setCode("SAVE10");
        request.setDiscountRate(new BigDecimal("0.20"));
        request.setUsageLimit(100);

        var publish = restTemplate.postForEntity("/api/coupons", request, Void.class);
        assertThat(publish.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var browse = restTemplate.getForEntity("/api/coupons", BrowseCouponsResponse.class);
        assertThat(browse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(browse.getBody()).isNotNull();
        assertThat(browse.getBody().coupons())
            .extracting(BrowseCouponsItemResponse::code)
            .contains("SAVE10");
    }
}
