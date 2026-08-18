package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.values.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class BrowseOrderHistoryResponse {

    private List<BrowseOrderHistoryItemResponse> orders;

    public List<BrowseOrderHistoryItemResponse> getOrders() {
        return orders;
    }

    public void setOrders(List<BrowseOrderHistoryItemResponse> orders) {
        this.orders = orders;
    }

    public static class BrowseOrderHistoryItemResponse {

        private String orderNumber;
        private Instant orderTimestamp;
        private String sku;
        private String country;
        private int quantity;
        private BigDecimal totalPrice;
        private OrderStatus status;
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

        public OrderStatus getStatus() {
            return status;
        }

        public void setStatus(OrderStatus status) {
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
