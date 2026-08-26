package com.mycompany.myshop.backend.usecases.commands.order;

import com.mycompany.myshop.backend.usecases.TypeValidationMessage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}
