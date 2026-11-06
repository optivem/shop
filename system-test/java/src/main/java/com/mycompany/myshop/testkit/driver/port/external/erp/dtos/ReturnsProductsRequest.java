package com.mycompany.myshop.testkit.driver.port.external.erp.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReturnsProductsRequest {
    private List<ReturnsProductRequest> products;
}
