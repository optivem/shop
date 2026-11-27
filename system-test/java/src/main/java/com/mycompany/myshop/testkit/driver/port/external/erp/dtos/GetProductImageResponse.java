package com.mycompany.myshop.testkit.driver.port.external.erp.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProductImageResponse {
    private String contentType;
    private int sizeInBytes;
}
