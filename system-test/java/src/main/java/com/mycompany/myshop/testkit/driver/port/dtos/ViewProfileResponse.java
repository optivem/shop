package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewProfileResponse {
    private String firstName;
    private String lastName;
    private String email;
    private String street;
    private String postalCode;
    private String city;
    private String country;
}
