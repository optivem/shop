package com.mycompany.myshop.backend.core.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
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
}
