package com.optivem.shop.dsl.driver.adapter.external.tax.client.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtGetCountryResponse {
    private String id;
    private String countryName;
    private BigDecimal taxRate;
}
