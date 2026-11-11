import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';
import { ErrorMessages } from '../../commons/constants/error-messages.js';
import { DELL_XPS, HP_15, LOGI_MX3S, givenTheCatalog } from './base/browse-catalog-background.js';
import { ANA_NOVAK_BERLIN, JANE_BROWN_LONDON, MARIE_DUPONT_PARIS, givenTheCatalogAndSavedAddress } from './base/check-out-background.js';

// Isolated: stages the shared ERP stub's product list and saves the shared test customer's profile.
// The cart lives in the browser and only the UI checks it out; there is no checkout API.
//
// Not covered: @UC-009.10 (first order: the address form opens prefilled, "Save as default" is ticked and saves the
// address). It needs a customer with no saved profile, and the two shared test customers' profiles can be saved but
// never cleared, so no scenario can rely on one being empty. The @UC-009.11 scenarios start from a saved address and
// choose "Ship to a different address": the same form and the same rules as on a first order.

const PLEASE_CORRECT_THE_ADDRESS = 'Please correct the shipping address.';
const NO_ORDER_WAS_PLACED = 'No order was placed.';

const missingFieldCases = [
    { label: 'first_name', field: 'first name', expectedError: 'First name is required' },
    { label: 'last_name', field: 'last name', expectedError: 'Last name is required' },
    { label: 'street', field: 'street', expectedError: 'Street is required' },
    { label: 'postal_code', field: 'postal code', expectedError: 'Postal code is required' },
    { label: 'city', field: 'city', expectedError: 'City is required' },
    { label: 'country', field: 'country', expectedError: 'Country is required' },
];

test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });

    forChannels(ChannelType.UI)(() => {
        test('shouldAskAnAnonymousVisitorToSignInBeforeCheckout', criteria('UC-009.7'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .and()
                .notLoggedIn()
                .when()
                .checkOut()
                .withCartContaining(HP_15, 1)
                .withProceededFromCart()
                .then()
                .shouldFail()
                .errorMessage(ErrorMessages.AUTHENTICATION_REQUIRED);
        });

        test('shouldReturnToTheCheckoutWithTheCartUnchangedAfterSignIn', criteria('UC-009.7'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .and()
                .notLoggedIn()
                .when()
                .checkOut()
                .withCartContaining(HP_15, 2)
                .withCartContaining(LOGI_MX3S, 1)
                .withProceededFromCartAndSignedIn()
                .then()
                .shouldSucceed()
                .checkout()
                .isShown()
                .hasReviewLines(HP_15, LOGI_MX3S)
                .hasReviewQuantity(HP_15, 2)
                .hasReviewQuantity(LOGI_MX3S, 1);
        });

        test('adminShouldNotBeAbleToCheckOut', criteria('UC-009.8'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .and()
                .loggedInAsAdmin()
                .when()
                .checkOut()
                .then()
                .shouldFail()
                .errorMessage(ErrorMessages.PERMISSION_DENIED);
        });

        test('shouldSendTheCustomerToTheCartWhenTheCartIsEmpty', criteria('UC-009.9'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .and()
                .loggedInAsCustomer()
                .when()
                .checkOut()
                .then()
                .shouldSucceed()
                .checkout()
                .showsTheCart()
                .showsMessage('Your cart is empty.');
        });

        test.each(missingFieldCases)(
            'shouldRejectAnIncompleteShippingAddress_$label',
            criteria('UC-009.11'),
            async ({ scenario, field, expectedError }) => {
                const address = JANE_BROWN_LONDON.withEmpty(field);
                await givenTheCatalogAndSavedAddress(scenario, ANA_NOVAK_BERLIN)
                    .when()
                    .checkOut()
                    .withCartContaining(HP_15, 1)
                    .withShippingAddress(address.firstName, address.lastName, address.street, address.postalCode, address.city, address.country)
                    .withOrderPlaced()
                    .then()
                    .shouldSucceed()
                    .checkout()
                    .isShown()
                    .confirmsNoOrder()
                    .showsMessage(PLEASE_CORRECT_THE_ADDRESS)
                    .showsMessageDetail(expectedError)
                    .hasCartLines(HP_15)
                    .hasCartLineQuantity(HP_15, 1);
            },
        );

        // The country select offers only US, DE and GB, so FR is sent the way an edited form would send it.
        test('shouldRejectAShippingCountryThatIsNotSupported', criteria('UC-009.11'), async ({ scenario }) => {
            const address = MARIE_DUPONT_PARIS;
            await givenTheCatalogAndSavedAddress(scenario, ANA_NOVAK_BERLIN)
                .when()
                .checkOut()
                .withCartContaining(HP_15, 1)
                .withShippingAddress(address.firstName, address.lastName, address.street, address.postalCode, address.city, address.country)
                .withOrderPlaced()
                .then()
                .shouldSucceed()
                .checkout()
                .isShown()
                .confirmsNoOrder()
                .showsMessage(PLEASE_CORRECT_THE_ADDRESS)
                .showsMessageDetail('Country must be US, DE or GB');
        });

        test('shouldStillPlaceTheOtherLinesWhenTheCouponRunsOutPartWay', criteria('UC-009.12'), async ({ scenario }) => {
            await givenTheCatalogAndSavedAddress(scenario, ANA_NOVAK_BERLIN)
                .coupon()
                .withCouponCode('LIMITED2024')
                .withUsageLimit(1)
                .when()
                .checkOut()
                .withCartContaining(HP_15, 1)
                .withCartContaining(DELL_XPS, 1)
                .withCouponCode('LIMITED2024')
                .withOrderPlaced()
                .then()
                .shouldSucceed()
                .checkout()
                .confirmsOrdersFor(HP_15)
                .confirmsRejected(DELL_XPS, 'Coupon code LIMITED2024 has exceeded its usage limit')
                .hasCartLines(DELL_XPS)
                .hasCartLineQuantity(DELL_XPS, 1)
                .and()
                .orderFor(HP_15)
                .hasAppliedCoupon('LIMITED2024');
        });

        test('shouldStillPlaceTheOtherLinesWhenAProductIsNoLongerOffered', criteria('UC-009.12'), async ({ scenario }) => {
            await givenTheCatalogAndSavedAddress(scenario, ANA_NOVAK_BERLIN)
                .when()
                .checkOut()
                .withCartContaining(DELL_XPS, 1)
                .withCartContaining(LOGI_MX3S, 2)
                .withErpNoLongerOffering(DELL_XPS)
                .withOrderPlaced()
                .then()
                .shouldSucceed()
                .checkout()
                .confirmsOrdersFor(LOGI_MX3S)
                .confirmsRejected(DELL_XPS, 'Product does not exist for SKU: DELL-XPS')
                .hasCartLines(DELL_XPS)
                .hasCartLineQuantity(DELL_XPS, 1);
        });

        test('shouldPlaceNothingAndKeepTheCartWhenEveryLineIsRejected', criteria('UC-009.13'), async ({ scenario }) => {
            await givenTheCatalogAndSavedAddress(scenario, ANA_NOVAK_BERLIN)
                .when()
                .checkOut()
                .withCartContaining(HP_15, 1)
                .withCartContaining(DELL_XPS, 1)
                .withCouponCode('INVALIDCOUPON')
                .withOrderPlaced()
                .then()
                .shouldSucceed()
                .checkout()
                .isShown()
                .confirmsNoOrder()
                .showsMessage(NO_ORDER_WAS_PLACED)
                .saysRejected(HP_15, 'Coupon code INVALIDCOUPON does not exist')
                .saysRejected(DELL_XPS, 'Coupon code INVALIDCOUPON does not exist')
                .hasCartLines(HP_15, DELL_XPS);
        });
    });
});
