package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenUpdateProfile;
import com.optivem.testing.Channel;
import com.optivem.testing.DataSource;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

import static com.mycompany.myshop.systemtest.commons.constants.ErrorMessages.AUTHENTICATION_REQUIRED;

// Isolated: the two test customers are shared by every scenario, so profile changes must not run in parallel.
@Isolated
class ManageProfileNegativeIsolatedTest extends BaseAcceptanceTest {
    @Tag("UC-010.2")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @DataSource({"firstName", "First name is required"})
    @DataSource({"lastName", "Last name is required"})
    @DataSource({"street", "Street is required"})
    @DataSource({"postalCode", "Postal code is required"})
    @DataSource({"city", "City is required"})
    @DataSource({"country", "Country is required"})
    void shouldRequireEveryNameAndAddressField(String field, String message) {
        var updateProfile = scenario
                .given().loggedInAsCustomer()
                .when().updateProfile();
        withField(updateProfile, field, "")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage(field, message);
    }

    @Tag("UC-010.2")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldTreatProfileFieldOfOnlySpacesAsEmpty() {
        scenario
                .given().loggedInAsCustomer()
                .when().updateProfile()
                    .withCity("   ")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("city", "City is required");
    }

    @Tag("UC-010.3")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectUnsupportedProfileCountry() {
        scenario
                .given().loggedInAsCustomer()
                .when().updateProfile()
                    .withCountry("FR")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("country", "Country must be US, DE or GB");
    }

    @Tag("UC-010.8")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldKeepSavedProfileWhenInvalidChangeIsRejected() {
        scenario
                .given().profile()
                    .withStreet("Main St 1")
                    .withPostalCode("10115")
                    .withCity("Berlin")
                    .withCountry("DE")
                    .savedByCustomer()
                .and().loggedInAsCustomer()
                .when().updateProfile()
                    .withStreet("")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("street", "Street is required")
                .and().profile()
                    .hasStreet("Main St 1")
                    .hasPostalCode("10115")
                    .hasCity("Berlin")
                    .hasCountry("DE");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void anonymousVisitorShouldNotBeAbleToViewProfile() {
        scenario
                .given().notLoggedIn()
                .when().viewProfile()
                .then().shouldFail()
                    .errorMessage(AUTHENTICATION_REQUIRED);
    }

    private static WhenUpdateProfile withField(WhenUpdateProfile updateProfile, String field, String value) {
        return switch (field) {
            case "firstName" -> updateProfile.withFirstName(value);
            case "lastName" -> updateProfile.withLastName(value);
            case "street" -> updateProfile.withStreet(value);
            case "postalCode" -> updateProfile.withPostalCode(value);
            case "city" -> updateProfile.withCity(value);
            case "country" -> updateProfile.withCountry(value);
            default -> throw new IllegalArgumentException("Unknown profile field: " + field);
        };
    }
}
