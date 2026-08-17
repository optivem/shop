package com.mycompany.myshop.backend.infrastructure.external.erp;

import lombok.Data;

import java.math.BigDecimal;

/**
 * The ERP's wire shape for the current promotion. See {@link ProductDetailsResponse}.
 */
@Data
public class GetPromotionResponse {
    private boolean promotionActive;
    private BigDecimal discount;
}
