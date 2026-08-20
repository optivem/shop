package com.mycompany.myshop.backend.infrastructure.external.tax;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

// ignoreUnknown because the tax provider owns this shape and can add to it without telling us. Without
// it, a field we do not read is a deserialization failure, which is the external system reaching past
// the boundary to break a request that never needed the new field.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxDetailsResponse {
    private String id;
    private String countryName;
    private BigDecimal taxRate;
}
