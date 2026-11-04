package com.mycompany.myshop.backend.core.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class BrowseCouponsResponse {
    private List<BrowseCouponsItemResponse> coupons;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

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
