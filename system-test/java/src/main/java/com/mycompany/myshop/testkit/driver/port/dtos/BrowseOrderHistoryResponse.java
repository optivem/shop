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
    /** UI only: the customer filter's choices as shown ("All customers", then usernames); null when there is no filter. */
    private List<String> customerFilterOptions;
    /** UI only: the customer filter's chosen option as shown, such as "All customers"; null when there is no filter. */
    private String customerFilterSelection;
    /** UI only: whether the list has a Customer column. */
    private Boolean customerColumnShown;
    /** UI only: the message the list shows instead of orders, such as "No orders found". */
    private String message;

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
        /** UI only: where the order's "View Details" link goes. */
        private String detailsPath;
    }
}
