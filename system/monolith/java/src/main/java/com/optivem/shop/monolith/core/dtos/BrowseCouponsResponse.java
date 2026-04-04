package com.optivem.shop.monolith.core.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class BrowseCouponsResponse {
    private List<BrowseCouponsItemResponse> coupons;

    @Data
    public static class BrowseCouponsItemResponse {
        private String code;
        private BigDecimal discountRate;
        private Instant validFrom;
        private Instant validTo;
        private Integer usageLimit;
        private Integer usedCount;
    }
}
