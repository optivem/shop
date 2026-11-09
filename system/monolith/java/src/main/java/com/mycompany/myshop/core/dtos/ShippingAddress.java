package com.mycompany.myshop.core.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The address an order is shipped to. Sent with a place-order request and returned with the order's
 * details. Its country is the order's tax country; it is not checked against a fixed list here but
 * goes through the tax lookup, like the Quick-order country.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Country is required")
    private String country;

    /** A copy with every field trimmed. */
    public ShippingAddress trimmed() {
        return new ShippingAddress(trim(firstName), trim(lastName), trim(street), trim(postalCode), trim(city), trim(country));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
