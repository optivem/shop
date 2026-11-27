package com.mycompany.myshop.core.dtos.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErpProductDetailsResponse {
    private String id;
    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private String brand;
    // Where the ERP serves the product photo, as a path on the ERP (e.g. /erp/images/HP-15.jpg); null when it has none.
    private String imageUrl;
}
