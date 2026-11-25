import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';

// UC-013: the visitor creates the account through the shop's "Sign up" link and Keycloak's registration form, so
// these scenarios run on the UI channel only. Every other scenario keeps creating customers through the admin API.
forChannels(ChannelType.UI)(() => {
    test('shouldTakeTheNameAndShippingCountryFromTheSignUp', criteria('UC-013.1'), async ({ scenario }) => {
        await scenario
            .given()
            .newCustomer()
            .withFirstName('Ana')
            .withLastName('Novak')
            .withCountry('GB')
            .signedUpThroughTheShop()
            .and()
            .loggedInAsCustomer()
            .when()
            .viewProfile()
            .then()
            .shouldSucceed()
            .profile()
            .hasFirstName('Ana')
            .hasLastName('Novak')
            .hasCountry('GB')
            .hasNoStreetAddress();
    });

    test('shouldLetTheNewCustomerPlaceAnOrderStraightAway', criteria('UC-013.2'), async ({ scenario }) => {
        await scenario
            .given()
            .newCustomer()
            .withCountry('GB')
            .signedUpThroughTheShop()
            .and()
            .loggedInAsCustomer()
            .when()
            .placeOrder()
            .withPrefilledCountry()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasCountryCode('GB');
    });
});
