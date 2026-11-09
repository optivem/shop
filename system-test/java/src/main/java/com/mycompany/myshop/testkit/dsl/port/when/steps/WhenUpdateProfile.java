package com.mycompany.myshop.testkit.dsl.port.when.steps;

import com.mycompany.myshop.testkit.dsl.port.when.steps.base.WhenStep;

/** Saves the acting user's profile; unset fields keep the defaults (Ana, Novak, Main St 1, 10115, Berlin, DE). */
public interface WhenUpdateProfile extends WhenStep {
    WhenUpdateProfile withFirstName(String firstName);

    WhenUpdateProfile withLastName(String lastName);

    WhenUpdateProfile withStreet(String street);

    WhenUpdateProfile withPostalCode(String postalCode);

    WhenUpdateProfile withCity(String city);

    WhenUpdateProfile withCountry(String country);
}
