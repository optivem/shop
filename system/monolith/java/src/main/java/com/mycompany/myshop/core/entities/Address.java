package com.mycompany.myshop.core.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A postal address as stored in a JSON column ({@code orders.shipping_address}). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String firstName;
    private String lastName;
    private String street;
    private String postalCode;
    private String city;
    private String country;
}
