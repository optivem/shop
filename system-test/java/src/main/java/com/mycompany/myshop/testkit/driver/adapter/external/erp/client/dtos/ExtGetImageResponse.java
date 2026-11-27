package com.mycompany.myshop.testkit.driver.adapter.external.erp.client.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtGetImageResponse {
    private String contentType;
    private int sizeInBytes;
}
