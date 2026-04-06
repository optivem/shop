package com.optivem.shop.dsl.driver.port.shop.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishCouponRequest {
    private String code;
    private String discountRate;
}
