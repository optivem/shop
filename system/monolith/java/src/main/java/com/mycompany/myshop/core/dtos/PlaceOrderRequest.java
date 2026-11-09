package com.mycompany.myshop.core.dtos;

import com.mycompany.myshop.core.validation.CountryRequiredWithoutShippingAddress;
import com.mycompany.myshop.core.validation.TypeValidationMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * With a {@link #shippingAddress} the tax country is the address's country and the top-level
 * {@link #country} is ignored; without one (Quick order) the top-level country is required.
 */
@Data
@CountryRequiredWithoutShippingAddress
public class PlaceOrderRequest {
    @NotBlank(message = "SKU must not be empty")
    private String sku;

    @NotNull(message = "Quantity must not be empty")
    @Positive(message = "Quantity must be positive")
    @TypeValidationMessage("Quantity must be an integer")
    private Integer quantity;

    private String country;

    private String couponCode;

    @Valid
    private ShippingAddress shippingAddress;
}
