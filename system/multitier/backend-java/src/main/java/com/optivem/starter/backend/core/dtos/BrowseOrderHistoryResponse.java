package com.optivem.starter.backend.core.dtos;

import com.optivem.starter.backend.core.entities.OrderStatus;
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
        private int quantity;
        private BigDecimal totalPrice;
        private OrderStatus status;
    }
}
