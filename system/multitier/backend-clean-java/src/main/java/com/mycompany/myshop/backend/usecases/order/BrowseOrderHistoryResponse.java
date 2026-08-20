package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.usecases.queries.OrderCursor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class BrowseOrderHistoryResponse {

    private List<BrowseOrderHistoryItemResponse> orders;
    private OrderCursor nextCursor;
    private boolean hasMore;

    public List<BrowseOrderHistoryItemResponse> getOrders() {
        return orders;
    }

    public void setOrders(List<BrowseOrderHistoryItemResponse> orders) {
        this.orders = orders;
    }

    // Typed, not encoded. This response is mapped by presentation before it reaches the wire, which
    // is where the cursor becomes an opaque token.
    public OrderCursor getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(OrderCursor nextCursor) {
        this.nextCursor = nextCursor;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public static class BrowseOrderHistoryItemResponse {

        private String orderNumber;
        private Instant orderTimestamp;
        private String sku;
        private String country;
        private int quantity;
        private BigDecimal totalPrice;
        private String status;
        private String appliedCouponCode;

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public Instant getOrderTimestamp() {
            return orderTimestamp;
        }

        public void setOrderTimestamp(Instant orderTimestamp) {
            this.orderTimestamp = orderTimestamp;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getAppliedCouponCode() {
            return appliedCouponCode;
        }

        public void setAppliedCouponCode(String appliedCouponCode) {
            this.appliedCouponCode = appliedCouponCode;
        }
    }
}
