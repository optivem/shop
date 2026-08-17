package com.mycompany.myshop.backend.core.dtos;

import com.mycompany.myshop.backend.core.validation.TypeValidationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PlaceOrderRequest {
    @NotBlank(message = "SKU must not be empty")
    private String sku;

    @NotNull(message = "Quantity must not be empty")
    @Positive(message = "Quantity must be positive")
    @TypeValidationMessage("Quantity must be an integer")
    private Integer quantity;

    @NotBlank(message = "Country must not be empty")
    private String country;

    private String couponCode;
}
