package com.optivem.eshop.dsl.driver.adapter.external.erp.client.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtProductDetailsResponse {
    private String id;
    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private String brand;
}
