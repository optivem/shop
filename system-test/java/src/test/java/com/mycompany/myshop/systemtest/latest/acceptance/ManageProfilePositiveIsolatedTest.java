package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

// Isolated: the two test customers are shared by every scenario, so profile changes must not run in parallel.
@Isolated
class ManageProfilePositiveIsolatedTest extends BaseAcceptanceTest {
    @Tag("UC-010.1")
    @TestTemplate
    @Channel(ChannelType.API)
    void shouldShowSavedProfile() {
        scenario
                .given().profile()
                    .withFirstName("Ana")
                    .withLastName("Novak")
                    .withStreet("Main St 1")
                    .withPostalCode("10115")
                    .withCity("Berlin")
                    .withCountry("DE")
                    .savedByCustomer()
                .and().loggedInAsCustomer()
                .when().viewProfile()
                .then().shouldSucceed()
                    .profile()
                        .hasFirstName("Ana")
                        .hasLastName("Novak")
                        .hasStreet("Main St 1")
                        .hasPostalCode("10115")
                        .hasCity("Berlin")
                        .hasCountry("DE")
                        .hasEmail("customer1@example.com");
    }

    @Tag("UC-010.1")
    @TestTemplate
    @Channel(ChannelType.API)
    void shouldShowChangedProfileAfterSaving() {
        scenario
                .given().profile()
                    .withCountry("DE")
                    .savedByCustomer()
                .and().loggedInAsCustomer()
                .when().updateProfile()
                    .withStreet("1 High St")
                    .withPostalCode("SW1A 1AA")
                    .withCity("London")
                    .withCountry("GB")
                .then().shouldSucceed()
                    .profile()
                        .hasStreet("1 High St")
                        .hasPostalCode("SW1A 1AA")
                        .hasCity("London")
                        .hasCountry("GB")
                    .and().profile()
                        .hasStreet("1 High St")
                        .hasPostalCode("SW1A 1AA")
                        .hasCity("London")
                        .hasCountry("GB");
    }

    @Tag("UC-010.5")
    @TestTemplate
    @Channel(ChannelType.API)
    void shouldKeepEachCustomersProfilePrivate() {
        scenario
                .given().profile()
                    .withStreet("Private Lane 7")
                    .savedByCustomer("A")
                .and().loggedInAsCustomer("B")
                .when().viewProfile()
                .then().shouldSucceed()
                    .profile()
                        .doesNotHaveStreet("Private Lane 7");
    }
}
