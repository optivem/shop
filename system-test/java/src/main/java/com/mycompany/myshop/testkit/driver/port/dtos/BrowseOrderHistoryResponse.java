package com.mycompany.myshop.testkit.driver.port.dtos;

import com.mycompany.myshop.testkit.common.domain.OrderStatus;

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
public class BrowseOrderHistoryResponse {
    private List<OrderDto> orders;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDto {
        private String orderNumber;
        private Instant orderTimestamp;
        private String sku;
        private String country;
        private int quantity;
        private BigDecimal totalPrice;
        private OrderStatus status;
        private String appliedCouponCode;
        private String customer;
    }
}
