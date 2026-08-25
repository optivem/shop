package com.mycompany.myshop.backend.core.dtos.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErpProductDetailsResponse {
    private String id;
    private BigDecimal price;
}
