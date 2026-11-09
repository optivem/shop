package com.mycompany.myshop.core.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileRequest {
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

    // A blank country matches the pattern so that only "Country is required" is reported for it.
    @NotBlank(message = "Country is required")
    @Pattern(regexp = "\\s*|US|DE|GB", message = "Country must be US, DE or GB")
    private String country;
}
