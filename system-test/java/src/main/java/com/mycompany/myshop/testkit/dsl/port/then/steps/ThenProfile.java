package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

/** Profile values are literals (no aliases): the profile only accepts US, DE or GB. */
public interface ThenProfile extends ThenStep<ThenProfile> {
    ThenProfile hasFirstName(String expectedFirstName);

    ThenProfile hasLastName(String expectedLastName);

    ThenProfile hasEmail(String expectedEmail);

    ThenProfile hasStreet(String expectedStreet);

    ThenProfile hasPostalCode(String expectedPostalCode);

    ThenProfile hasCity(String expectedCity);

    ThenProfile hasCountry(String expectedCountry);

    ThenProfile doesNotHaveStreet(String street);

    /** No shipping address is saved: the street, postal code, city and country are empty. */
    ThenProfile hasNoSavedAddress();
}
