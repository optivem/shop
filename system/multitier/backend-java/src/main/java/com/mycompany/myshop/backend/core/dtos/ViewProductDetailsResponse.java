package com.mycompany.myshop.backend.core.dtos;

import lombok.Data;

import java.math.BigDecimal;

/** One catalog product, as served by GET /api/products and GET /api/products/{sku}. */
@Data
public class ViewProductDetailsResponse {
    private String sku;
    private String title;
    private String description;
    private String brand;
    private String category;
    private BigDecimal price;
}
