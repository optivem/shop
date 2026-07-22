package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
        private BigDecimal discountRate;
        private Instant validFrom;
        private Instant validTo;
        private Integer usageLimit;
        private Integer usedCount;
    }
}
