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

    /** The email is shown but cannot be edited: it comes from the sign-in account (UI only). */
    ThenProfile hasEmailThatCannotBeChanged();

    /** The country select offers exactly these countries, in this order (UI only). */
    ThenProfile hasCountryChoices(String... expectedCountries);

    /** The profile's note on how sales tax is charged (UI only). */
    ThenProfile hasTaxNotice(String expectedNotice);

    /** The customer was told this after saving (UI only). */
    ThenProfile hasMessage(String expectedMessage);
}
