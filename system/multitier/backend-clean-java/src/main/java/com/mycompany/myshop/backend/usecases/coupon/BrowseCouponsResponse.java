package com.mycompany.myshop.backend.usecases.coupon;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class BrowseCouponsResponse {

    private List<BrowseCouponsItemResponse> coupons;

    public List<BrowseCouponsItemResponse> getCoupons() {
        return coupons;
    }

    public void setCoupons(List<BrowseCouponsItemResponse> coupons) {
        this.coupons = coupons;
    }

    public static class BrowseCouponsItemResponse {

        private String code;
        private BigDecimal discountRate;
        private Instant validFrom;
        private Instant validTo;
        private Integer usageLimit;
        private Integer usedCount;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public BigDecimal getDiscountRate() {
            return discountRate;
        }

        public void setDiscountRate(BigDecimal discountRate) {
            this.discountRate = discountRate;
        }

        public Instant getValidFrom() {
            return validFrom;
        }

        public void setValidFrom(Instant validFrom) {
            this.validFrom = validFrom;
        }

        public Instant getValidTo() {
            return validTo;
        }

        public void setValidTo(Instant validTo) {
            this.validTo = validTo;
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
    }
}
