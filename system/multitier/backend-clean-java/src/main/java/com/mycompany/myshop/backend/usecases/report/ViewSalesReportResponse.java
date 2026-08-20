package com.mycompany.myshop.backend.usecases.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ViewSalesReportResponse {

    private List<RevenueByCountryMonthResponse> revenueByCountryMonth;
    private List<TopSkuResponse> topSkus;
    private List<CouponEffectivenessResponse> couponEffectiveness;

    public List<RevenueByCountryMonthResponse> getRevenueByCountryMonth() {
        return revenueByCountryMonth;
    }

    public void setRevenueByCountryMonth(List<RevenueByCountryMonthResponse> revenueByCountryMonth) {
        this.revenueByCountryMonth = revenueByCountryMonth;
    }

    public List<TopSkuResponse> getTopSkus() {
        return topSkus;
    }

    public void setTopSkus(List<TopSkuResponse> topSkus) {
        this.topSkus = topSkus;
    }

    public List<CouponEffectivenessResponse> getCouponEffectiveness() {
        return couponEffectiveness;
    }

    public void setCouponEffectiveness(List<CouponEffectivenessResponse> couponEffectiveness) {
        this.couponEffectiveness = couponEffectiveness;
    }

    public static class RevenueByCountryMonthResponse {

        private String country;
        private Instant month;
        private long orderCount;
        private long quantity;
        private BigDecimal subtotalPrice;
        private BigDecimal taxAmount;
        private BigDecimal totalPrice;

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public Instant getMonth() {
            return month;
        }

        public void setMonth(Instant month) {
            this.month = month;
        }

        public long getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(long orderCount) {
            this.orderCount = orderCount;
        }

        public long getQuantity() {
            return quantity;
        }

        public void setQuantity(long quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getSubtotalPrice() {
            return subtotalPrice;
        }

        public void setSubtotalPrice(BigDecimal subtotalPrice) {
            this.subtotalPrice = subtotalPrice;
        }

        public BigDecimal getTaxAmount() {
            return taxAmount;
        }

        public void setTaxAmount(BigDecimal taxAmount) {
            this.taxAmount = taxAmount;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
        }
    }

    public static class TopSkuResponse {

        private String sku;
        private long orderCount;
        private long quantity;
        private BigDecimal totalPrice;

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public long getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(long orderCount) {
            this.orderCount = orderCount;
        }

        public long getQuantity() {
            return quantity;
        }

        public void setQuantity(long quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
        }
    }

    public static class CouponEffectivenessResponse {

        private String code;
        private Integer usageLimit;
        private Integer usedCount;
        private long orderCount;
        private BigDecimal discountAmount;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Integer getUsageLimit() {
            return usageLimit;
        }

        public void setUsageLimit(Integer usageLimit) {
            this.usageLimit = usageLimit;
        }

        public Integer getUsedCount() {
            return usedCount;
        }

        public void setUsedCount(Integer usedCount) {
            this.usedCount = usedCount;
        }

        public long getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(long orderCount) {
            this.orderCount = orderCount;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public void setDiscountAmount(BigDecimal discountAmount) {
            this.discountAmount = discountAmount;
        }
    }
}
