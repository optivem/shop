package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.ViewProfileResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies a profile as returned by viewing or saving it. Values are literals (no aliases). */
public class ViewProfileVerification extends ResponseVerification<ViewProfileResponse> {
    public ViewProfileVerification(ViewProfileResponse response, UseCaseContext context) {
        super(response, context);
    }

    public ViewProfileVerification firstName(String expectedFirstName) {
        var actualFirstName = getResponse().getFirstName();
        assertThat(actualFirstName)
                .withFailMessage("Expected profile first name to be '%s', but was '%s'", expectedFirstName, actualFirstName)
                .isEqualTo(expectedFirstName);
        return this;
    }

    public ViewProfileVerification lastName(String expectedLastName) {
        var actualLastName = getResponse().getLastName();
        assertThat(actualLastName)
                .withFailMessage("Expected profile last name to be '%s', but was '%s'", expectedLastName, actualLastName)
                .isEqualTo(expectedLastName);
        return this;
    }

    public ViewProfileVerification email(String expectedEmail) {
        var actualEmail = getResponse().getEmail();
        assertThat(actualEmail)
                .withFailMessage("Expected profile email to be '%s', but was '%s'", expectedEmail, actualEmail)
                .isEqualTo(expectedEmail);
        return this;
    }

    public ViewProfileVerification street(String expectedStreet) {
        var actualStreet = getResponse().getStreet();
        assertThat(actualStreet)
                .withFailMessage("Expected profile street to be '%s', but was '%s'", expectedStreet, actualStreet)
                .isEqualTo(expectedStreet);
        return this;
    }

    public ViewProfileVerification postalCode(String expectedPostalCode) {
        var actualPostalCode = getResponse().getPostalCode();
        assertThat(actualPostalCode)
                .withFailMessage("Expected profile postal code to be '%s', but was '%s'", expectedPostalCode, actualPostalCode)
                .isEqualTo(expectedPostalCode);
        return this;
    }

    public ViewProfileVerification city(String expectedCity) {
        var actualCity = getResponse().getCity();
        assertThat(actualCity)
                .withFailMessage("Expected profile city to be '%s', but was '%s'", expectedCity, actualCity)
                .isEqualTo(expectedCity);
        return this;
    }

    public ViewProfileVerification country(String expectedCountry) {
        var actualCountry = getResponse().getCountry();
        assertThat(actualCountry)
                .withFailMessage("Expected profile country to be '%s', but was '%s'", expectedCountry, actualCountry)
                .isEqualTo(expectedCountry);
        return this;
    }

    /** The street, postal code, city and country are all empty (an empty field reads as null or ""). */
    public ViewProfileVerification addressIsEmpty() {
        var response = getResponse();
        assertThat(new String[] {response.getStreet(), response.getPostalCode(), response.getCity(), response.getCountry()})
                .withFailMessage("Expected no saved address, but the profile has street '%s', postal code '%s', city '%s', country '%s'",
                        response.getStreet(), response.getPostalCode(), response.getCity(), response.getCountry())
                .allMatch(value -> value == null || value.isEmpty());
        return this;
    }

    public ViewProfileVerification streetIsNot(String unexpectedStreet) {
        var actualStreet = getResponse().getStreet();
        assertThat(actualStreet)
                .withFailMessage("Expected profile street not to be '%s', but it was", unexpectedStreet)
                .isNotEqualTo(unexpectedStreet);
        return this;
    }
}
