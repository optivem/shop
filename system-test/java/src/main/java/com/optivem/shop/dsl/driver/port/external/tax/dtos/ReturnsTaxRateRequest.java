package com.optivem.shop.dsl.driver.port.external.tax.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnsTaxRateRequest {
    private String country;
    private String taxRate;
}
