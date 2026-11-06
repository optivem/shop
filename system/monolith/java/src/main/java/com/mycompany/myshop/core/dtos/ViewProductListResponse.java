package com.mycompany.myshop.core.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ViewProductListResponse {
    private List<ViewProductListItemResponse> products;

    @Data
    public static class ViewProductListItemResponse {
        private String sku;
        private BigDecimal price;
    }
}
