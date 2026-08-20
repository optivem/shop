package com.mycompany.myshop.backend.infrastructure.external.erp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

// See TaxDetailsResponse: every wire DTO in this package tolerates fields it does not read, so a
// supplier adding one is a non-event rather than an outage.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class GetPromotionResponse {
    private boolean promotionActive;
    private BigDecimal discount;
}
