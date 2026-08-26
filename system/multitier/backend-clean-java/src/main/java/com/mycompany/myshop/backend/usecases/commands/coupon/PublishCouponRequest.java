package com.mycompany.myshop.backend.usecases.commands.coupon;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public class PublishCouponRequest {

    @NotBlank(message = "Coupon code must not be blank")
    private String code;

    @NotNull(message = "Discount rate must not be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount rate must be greater than 0.00")
    @DecimalMax(value = "1.0", message = "Discount rate must be at most 1.00")
    private BigDecimal discountRate;

    private Instant validFrom;

    private Instant validTo;

    @Positive(message = "Usage limit must be positive")
    private Integer usageLimit;

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
}
