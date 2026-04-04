package com.optivem.shop.backend.core.dtos.external;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaxDetailsResponse {
    private String id;
    private String countryName;
    private BigDecimal taxRate;
}
