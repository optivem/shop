package com.mycompany.myshop.backend.usecases.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class BrowseOrderHistoryResponse {

    private List<BrowseOrderHistoryItemResponse> orders;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public List<BrowseOrderHistoryItemResponse> getOrders() {
        return orders;
    }

    public void setOrders(List<BrowseOrderHistoryItemResponse> orders) {
        this.orders = orders;
    }

    // The page that was served, not the page that was asked for. They differ when the request left
    // it out.
    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    // The two numbers a paged UI is built from: how many rows match, and therefore how many buttons
    // to draw. totalPages is carried rather than left to the client to divide, so that every client
    // rounds the same way.
    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
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
