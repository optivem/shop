import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';

// The two test customers are shared by every test, so profile tests must not run in parallel.
test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });
    forChannels(ChannelType.UI, ChannelType.API)(() => {
        test('shouldShowSavedProfile', criteria('UC-010.1'), async ({ scenario }) => {
            await scenario
                .given()
                .profile()
                .withFirstName('Ana')
                .withLastName('Novak')
                .withStreet('Main St 1')
                .withPostalCode('10115')
                .withCity('Berlin')
                .withCountry('DE')
                .savedByCustomer()
                .and()
                .loggedInAsCustomer()
                .when()
                .viewProfile()
                .then()
                .shouldSucceed()
                .profile()
                .hasFirstName('Ana')
                .hasLastName('Novak')
                .hasStreet('Main St 1')
                .hasPostalCode('10115')
                .hasCity('Berlin')
                .hasCountry('DE')
                .hasEmail('customer1@example.com');
        });

        test('shouldShowChangedProfileAfterSaving', criteria('UC-010.1'), async ({ scenario }) => {
            await scenario
                .given()
                .profile()
                .withCountry('DE')
                .savedByCustomer()
                .and()
                .loggedInAsCustomer()
                .when()
                .updateProfile()
                .withStreet('1 High St')
                .withPostalCode('SW1A 1AA')
                .withCity('London')
                .withCountry('GB')
                .then()
                .shouldSucceed()
                .and()
                .profile()
                .hasStreet('1 High St')
                .hasPostalCode('SW1A 1AA')
                .hasCity('London')
                .hasCountry('GB');
        });

        test('shouldKeepEachCustomersProfilePrivate', criteria('UC-010.5'), async ({ scenario }) => {
            await scenario
                .given()
                .profile()
                .withStreet('Private Lane 7')
                .savedByCustomer('A')
                .and()
                .loggedInAsCustomer('B')
                .when()
                .viewProfile()
                .then()
                .shouldSucceed()
                .profile()
                .doesNotHaveStreet('Private Lane 7');
        });

        // The new customer's email is unique to the run, so the email shown is checked by shouldShowSavedProfile.
        test('shouldShowAnEmptyProfileWithNamesFromTheSignInAccount', criteria('UC-010.7'), async ({ scenario }) => {
            await scenario
                .given()
                .newCustomer()
                .withFirstName('Ana')
                .withLastName('Novak')
                .and()
                .loggedInAsCustomer()
                .when()
                .viewProfile()
                .then()
                .shouldSucceed()
                .profile()
                .hasFirstName('Ana')
                .hasLastName('Novak')
                .hasNoSavedAddress();
        });
    });

    forChannels(ChannelType.UI)(() => {
        test('shouldNotLetTheCustomerChangeTheEmail', criteria('UC-010.1'), async ({ scenario }) => {
            await scenario
                .given()
                .loggedInAsCustomer()
                .when()
                .viewProfile()
                .then()
                .shouldSucceed()
                .profile()
                .hasEmail('customer1@example.com')
                .hasEmailThatCannotBeChanged();
        });

        test('shouldTellTheCustomerTheProfileWasSaved', criteria('UC-010.1'), async ({ scenario }) => {
            await scenario
                .given()
                .loggedInAsCustomer()
                .when()
                .updateProfile()
                .then()
                .shouldSucceed()
                .profile()
                .hasMessage('Your profile has been saved.');
        });

        test('shouldOfferOnlyTheSupportedShippingCountries', criteria('UC-010.3'), async ({ scenario }) => {
            await scenario
                .given()
                .loggedInAsCustomer()
                .when()
                .viewProfile()
                .then()
                .shouldSucceed()
                .profile()
                .hasCountryChoices('US', 'DE', 'GB');
        });

        test('shouldTellTheCustomerThatTaxFollowsTheShippingCountry', criteria('UC-010.4'), async ({ scenario }) => {
            await scenario
                .given()
                .loggedInAsCustomer()
                .when()
                .viewProfile()
                .then()
                .shouldSucceed()
                .profile()
                .hasTaxNotice("Sales tax is charged at the rate of your shipping address's country.");
        });

        // The saved address as checkout's default (the other @UC-010.5 scenario) is
        // check-out-positive-isolated-test's shouldPreselectTheSavedShippingAddress.
        test('shouldPrefillTheQuickOrderCountryFromTheProfile', criteria('UC-010.5'), async ({ scenario }) => {
            await scenario
                .given()
                .profile()
                .withCountry('GB')
                .savedByCustomer()
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

        test('shouldAskAnAnonymousVisitorToSignInAndReturnToTheProfile', criteria('UC-010.6'), async ({ scenario }) => {
            await scenario
                .given()
                .notLoggedIn()
                .when()
                .viewProfile()
                .withSignInWhenAsked()
                .then()
                .shouldSucceed()
                .profile()
                .hasEmail('customer1@example.com');
        });
    });
});
