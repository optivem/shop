package com.optivem.shop.dsl.driver.port.shop.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowseCouponsResponse {
    private List<CouponDto> coupons;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponDto {
        private String code;
        private double discountRate;
        private Instant validFrom;
        private Instant validTo;
        private Integer usageLimit;
        private Integer usedCount;
    }
}
