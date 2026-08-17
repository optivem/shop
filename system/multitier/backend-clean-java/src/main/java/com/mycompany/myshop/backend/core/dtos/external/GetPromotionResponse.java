package com.mycompany.myshop.backend.core.dtos.external;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class GetPromotionResponse {
    private boolean promotionActive;
    private BigDecimal discount;
}
