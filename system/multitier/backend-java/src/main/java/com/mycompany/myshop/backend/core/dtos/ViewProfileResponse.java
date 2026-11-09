package com.mycompany.myshop.backend.core.dtos;

import lombok.Data;

@Data
public class ViewProfileResponse {
    private String firstName;
    private String lastName;
    private String email;
    private String street;
    private String postalCode;
    private String city;
    private String country;
}
