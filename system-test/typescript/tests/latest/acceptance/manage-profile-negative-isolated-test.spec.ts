import { test, forChannels, ChannelType } from './base/fixtures.js';
import { ErrorMessages } from '../../commons/constants/error-messages.js';
import { criteria } from '../../commons/criteria.js';

// The two test customers are shared by every test, so profile tests must not run in parallel.
test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });
    forChannels(ChannelType.UI, ChannelType.API)(() => {
        const requiredFieldCases = [
            { field: 'firstName', message: 'First name is required' },
            { field: 'lastName', message: 'Last name is required' },
            { field: 'street', message: 'Street is required' },
            { field: 'postalCode', message: 'Postal code is required' },
            { field: 'city', message: 'City is required' },
            { field: 'country', message: 'Country is required' },
        ] as const;

        test.each(requiredFieldCases)(
            'shouldRequireEveryNameAndAddressField_field=$field',
            criteria('UC-010.2'),
            async ({ scenario, field, message }) => {
                const update = scenario.given().loggedInAsCustomer().when().updateProfile();
                switch (field) {
                    case 'firstName':
                        update.withFirstName('');
                        break;
                    case 'lastName':
                        update.withLastName('');
                        break;
                    case 'street':
                        update.withStreet('');
                        break;
                    case 'postalCode':
                        update.withPostalCode('');
                        break;
                    case 'city':
                        update.withCity('');
                        break;
                    case 'country':
                        update.withCountry('');
                        break;
                }
                await update
                    .then()
                    .shouldFail()
                    .errorMessage('The request contains one or more validation errors')
                    .fieldErrorMessage(field, message);
            },
        );

        test('shouldTreatProfileFieldOfOnlySpacesAsEmpty', criteria('UC-010.2'), async ({ scenario }) => {
            await scenario
                .given()
                .loggedInAsCustomer()
                .when()
                .updateProfile()
                .withCity('   ')
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('city', 'City is required');
        });

        test('shouldRejectUnsupportedProfileCountry', criteria('UC-010.3'), async ({ scenario }) => {
            await scenario
                .given()
                .loggedInAsCustomer()
                .when()
                .updateProfile()
                .withCountry('FR')
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('country', 'Country must be US, DE or GB');
        });

        test('shouldKeepSavedProfileWhenInvalidChangeIsRejected', criteria('UC-010.8'), async ({ scenario }) => {
            await scenario
                .given()
                .profile()
                .withStreet('Main St 1')
                .withPostalCode('10115')
                .withCity('Berlin')
                .withCountry('DE')
                .savedByCustomer()
                .and()
                .loggedInAsCustomer()
                .when()
                .updateProfile()
                .withStreet('')
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('street', 'Street is required')
                .and()
                .profile()
                .hasStreet('Main St 1')
                .hasPostalCode('10115')
                .hasCity('Berlin')
                .hasCountry('DE');
        });

        test('anonymousVisitorShouldNotBeAbleToViewProfile', async ({ scenario }) => {
            await scenario
                .given()
                .notLoggedIn()
                .when()
                .viewProfile()
                .then()
                .shouldFail()
                .errorMessage(ErrorMessages.AUTHENTICATION_REQUIRED);
        });
    });
});
