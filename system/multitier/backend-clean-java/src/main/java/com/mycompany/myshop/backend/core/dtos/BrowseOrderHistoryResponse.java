package com.mycompany.myshop.backend.core.dtos;

import com.mycompany.myshop.backend.core.entities.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class BrowseOrderHistoryResponse {
    private List<BrowseOrderHistoryItemResponse> orders;

    @Data
    public static class BrowseOrderHistoryItemResponse {
        private String orderNumber;
        private Instant orderTimestamp;
        private String sku;
        private String country;
        private int quantity;
        private BigDecimal totalPrice;
        private OrderStatus status;
        private String appliedCouponCode;
    }
}
