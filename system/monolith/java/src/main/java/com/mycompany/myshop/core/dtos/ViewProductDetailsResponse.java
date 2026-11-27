package com.mycompany.myshop.core.dtos;

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
    // The shop's own URL for the product photo (GET /api/products/{sku}/image); null when the ERP has none.
    private String imageUrl;
}
