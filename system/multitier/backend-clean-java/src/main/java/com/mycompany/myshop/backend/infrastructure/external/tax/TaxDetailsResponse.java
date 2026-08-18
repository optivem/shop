package com.mycompany.myshop.backend.infrastructure.external.tax;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaxDetailsResponse {
    private String id;
    private String countryName;
    private BigDecimal taxRate;
}
