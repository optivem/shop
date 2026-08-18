package com.mycompany.myshop.backend.infrastructure.external.erp;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GetPromotionResponse {
    private boolean promotionActive;
    private BigDecimal discount;
}
