package com.mycompany.myshop.testkit.driver.port.external.erp.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetProductImageRequest {
    /** The image URL as the ERP gave it on the product, resolved against the ERP base URL. */
    private String imageUrl;
}
