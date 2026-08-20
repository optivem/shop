package com.mycompany.myshop.backend.infrastructure.external.erp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class ProductDetailsResponse {
    private String id;
    private BigDecimal price;
}
