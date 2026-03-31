package com.optivem.eshop.dsl.driver.port.external.erp.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProductResponse {
    private String sku;
    private BigDecimal price;
}
