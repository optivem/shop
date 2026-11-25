package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

// UC-013: the visitor creates the account through the shop's "Sign up" link and Keycloak's registration form, so
// these scenarios run on the UI channel only. Every other scenario keeps creating customers through the admin API.
class CreateCustomerPositiveTest extends BaseAcceptanceTest {
    @Tag("UC-013.1")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldTakeTheNameAndShippingCountryFromTheSignUp() {
        scenario
                .given().newCustomer()
                    .withFirstName("Ana")
                    .withLastName("Novak")
                    .withCountry("GB")
                    .signedUpThroughTheShop()
                .and().loggedInAsCustomer()
                .when().viewProfile()
                .then().shouldSucceed()
                    .profile()
                        .hasFirstName("Ana")
                        .hasLastName("Novak")
                        .hasCountry("GB")
                        .hasNoStreetAddress();
    }

    @Tag("UC-013.2")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldLetTheNewCustomerPlaceAnOrderStraightAway() {
        scenario
                .given().newCustomer()
                    .withCountry("GB")
                    .signedUpThroughTheShop()
                .and().loggedInAsCustomer()
                .when().placeOrder()
                    .withPrefilledCountry()
                .then().shouldSucceed()
                .and().order()
                    .hasCountryCode("GB");
    }
}
