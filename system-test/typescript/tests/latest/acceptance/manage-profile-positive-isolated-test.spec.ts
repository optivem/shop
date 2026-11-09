import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';

// The two test customers are shared by every test, so profile tests must not run in parallel.
test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });
    forChannels(ChannelType.API)(() => {
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
    });
});
