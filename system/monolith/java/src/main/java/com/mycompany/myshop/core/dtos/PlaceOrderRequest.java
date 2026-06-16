package com.mycompany.myshop.core.dtos;

import com.mycompany.myshop.core.validation.TypeValidationMessage;
import jakarta.validation.constraints.Max;
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
    @Max(value = 99, message = "Quantity must be less than 100")
    @TypeValidationMessage("Quantity must be an integer")
    private Integer quantity;

    @NotBlank(message = "Country must not be empty")
    private String country;

    private String couponCode;
}
