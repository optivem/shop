package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    /** UI only: whether the email field can be edited (null on the API, which has no email input). */
    private Boolean emailChangeable;
    /** UI only: the countries the country select offers, as codes, without its empty "Select a country" choice. */
    private List<String> countryChoices;
    /** UI only: the note on how sales tax is charged. */
    private String taxNotice;
    /** UI only: the notification shown after saving, such as "Your profile has been saved.". */
    private String message;
}
