package com.optivem.shop.dsl.driver.port.shop.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowseCouponsResponse {
    private List<BrowseCouponsItemResponse> coupons;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BrowseCouponsItemResponse {
        private String code;
        private BigDecimal discountRate;
    }
}
