package com.mycompany.myshop.backend.infrastructure.external.tax;

import lombok.Data;

import java.math.BigDecimal;

/**
 * The tax system's wire shape for a country. Lives in the adapter, never crosses into the domain.
 */
@Data
public class TaxDetailsResponse {
    private String id;
    private String countryName;
    private BigDecimal taxRate;
}
