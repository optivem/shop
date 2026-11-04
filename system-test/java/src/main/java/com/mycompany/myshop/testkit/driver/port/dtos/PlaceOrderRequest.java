package com.mycompany.myshop.testkit.driver.port.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {
    private String sku;
    private String quantity;
    private String country;
    private String couponCode;
    /** Sent as the {@code Idempotency-Key} header, never in the body; null means no header. */
    @JsonIgnore
    private String idempotencyKey;
}
