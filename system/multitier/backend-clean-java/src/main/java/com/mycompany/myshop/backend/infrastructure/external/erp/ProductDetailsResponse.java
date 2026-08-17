package com.mycompany.myshop.backend.infrastructure.external.erp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * The ERP's wire shape for a product. Lives in the adapter, never crosses into the domain — a
 * supplier renaming a JSON field stops here.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDetailsResponse {
    private String id;
    private BigDecimal price;
}
