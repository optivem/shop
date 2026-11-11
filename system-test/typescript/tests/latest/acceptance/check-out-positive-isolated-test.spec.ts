import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';
import { DELL_XPS, HP_15, LOGI_MX3S } from './base/browse-catalog-background.js';
import { ANA_NOVAK_BERLIN, JANE_BROWN_LONDON, JOHN_SMITH_NEW_YORK, givenTheCatalogAndSavedAddress } from './base/check-out-background.js';

// Isolated: stages the shared ERP stub's product list and saves the shared test customer's profile.
// The cart lives in the browser and only the UI checks it out; there is no checkout API. The orders it places are
// checked through the order API.

const SUMMER2025 = 'SUMMER2025';

const taxCases = [
    { label: 'US', address: JOHN_SMITH_NEW_YORK, country: 'US', taxRate: '0.00' },
    { label: 'DE', address: ANA_NOVAK_BERLIN, country: 'DE', taxRate: '0.19' },
    { label: 'GB', address: JANE_BROWN_LONDON, country: 'GB', taxRate: '0.20' },
];

test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });

    forChannels(ChannelType.UI)(() => {
        test('shouldPlaceOneOrderPerCartLine', criteria('UC-009.1'), async ({ scenario }) => {
            const address = ANA_NOVAK_BERLIN;
            await givenTheCatalogAndSavedAddress(scenario, address)
                .when()
                .checkOut()
                .withCartContaining(HP_15, 2)
                .withCartContaining(DELL_XPS, 1)
                .withOrderPlaced()
                .then()
                .shouldSucceed()
                .checkout()
                .confirmsOrdersFor(HP_15, DELL_XPS)
                .and()
                .orderFor(HP_15)
                .hasQuantity(2)
                .hasShippingAddressWithCountryCode(address.firstName, address.lastName, address.street, address.postalCode, address.city, address.country)
                .and()
                .orderFor(DELL_XPS)
                .hasQuantity(1)
                .hasShippingAddressWithCountryCode(address.firstName, address.lastName, address.street, address.postalCode, address.city, address.country);
        });

        // The confirmation lists the orders in the order they were placed.
        test('shouldPlaceTheOrdersInCartOrder', criteria('UC-009.1'), async ({ scenario }) => {
            await givenTheCatalogAndSavedAddress(scenario, ANA_NOVAK_BERLIN)
                .when()
                .checkOut()
                .withCartContaining(LOGI_MX3S, 1)
                .withCartContaining(HP_15, 1)
                .withOrderPlaced()
                .then()
                .shouldSucceed()
                .checkout()
                .confirmsOrdersFor(LOGI_MX3S, HP_15);
        });

        test.each(taxCases)(
            'shouldApplyTheTaxRateOfTheShippingAddressesCountry_$label',
            criteria('UC-009.2'),
            async ({ scenario, address, country, taxRate }) => {
                await givenTheCatalogAndSavedAddress(scenario, address)
                    .when()
                    .checkOut()
                    .withCartContaining(LOGI_MX3S, 1)
                    .withOrderPlaced()
                    .then()
                    .shouldSucceed()
                    .orderFor(LOGI_MX3S)
                    .hasCountryCode(country)
                    .hasTaxRate(taxRate);
            },
        );

        test('shouldShowTheEstimatedTaxForTheChosenShippingCountry', criteria('UC-009.2'), async ({ scenario }) => {
            const other = JANE_BROWN_LONDON;
            await givenTheCatalogAndSavedAddress(scenario, ANA_NOVAK_BERLIN)
                .when()
                .checkOut()
                .withCartContaining(HP_15, 1)
                .withShippingAddress(other.firstName, other.lastName, other.street, other.postalCode, other.city, other.country)
                .then()
                .shouldSucceed()
                .checkout()
                .hadTaxWhenOpened('DE', '19%')
                .hasTax('GB', '20%');
        });

        test('shouldPreselectTheSavedShippingAddress', criteria('UC-009.3'), async ({ scenario }) => {
            await givenTheCatalogAndSavedAddress(scenario, ANA_NOVAK_BERLIN)
                .when()
                .checkOut()
                .withCartContaining(HP_15, 1)
                .then()
                .shouldSucceed()
                .checkout()
                .hasSavedAddressSelected(ANA_NOVAK_BERLIN.formatted());
        });

        test('shouldShipToADifferentAddressWithoutChangingTheProfile', criteria('UC-009.3'), async ({ scenario }) => {
            const saved = ANA_NOVAK_BERLIN;
            const other = JANE_BROWN_LONDON;
            await givenTheCatalogAndSavedAddress(scenario, saved)
                .when()
                .checkOut()
                .withCartContaining(HP_15, 1)
                .withShippingAddress(other.firstName, other.lastName, other.street, other.postalCode, other.city, other.country)
                .withOrderPlaced()
                .then()
                .shouldSucceed()
                .orderFor(HP_15)
                .hasShippingAddressWithCountryCode(other.firstName, other.lastName, other.street, other.postalCode, other.city, other.country)
                .hasTaxRate('0.20')
                .and()
                .profile()
                .hasFirstName(saved.firstName)
                .hasLastName(saved.lastName)
                .hasStreet(saved.street)
                .hasPostalCode(saved.postalCode)
                .hasCity(saved.city)
                .hasCountry(saved.country);
        });

        test('shouldApplyTheCouponToEveryOrder', criteria('UC-009.4'), async ({ scenario }) => {
            await givenTheCatalogAndSavedAddress(scenario, JOHN_SMITH_NEW_YORK)
                .coupon()
                .withCouponCode(SUMMER2025)
                .withDiscountRate('0.15')
                .when()
                .checkOut()
                .withCartContaining(HP_15, 1)
                .withCartContaining(LOGI_MX3S, 1)
                .withCouponCode(SUMMER2025)
                .withOrderPlaced()
                .then()
                .shouldSucceed()
                .checkout()
                .confirmsOrdersFor(HP_15, LOGI_MX3S)
                .and()
                .orderFor(HP_15)
                .hasAppliedCoupon(SUMMER2025)
                .hasDiscountRate('0.15')
                .and()
                .orderFor(LOGI_MX3S)
                .hasAppliedCoupon(SUMMER2025)
                .hasDiscountRate('0.15')
                .and()
                .coupon(SUMMER2025)
                .hasUsedCount(2);
        });

        test('shouldApplyNoDiscountWhenThereIsNoCoupon', criteria('UC-009.4'), async ({ scenario }) => {
            await givenTheCatalogAndSavedAddress(scenario, JOHN_SMITH_NEW_YORK)
                .when()
                .checkOut()
                .withCartContaining(HP_15, 1)
                .withOrderPlaced()
                .then()
                .shouldSucceed()
                .orderFor(HP_15)
                .hasAppliedCoupon(null)
                .hasDiscountAmount('0.00');
        });

        test('shouldReviewTheCartLinesAndSubtotalAtCatalogPrices', criteria('UC-009.5'), async ({ scenario }) => {
            await givenTheCatalogAndSavedAddress(scenario, ANA_NOVAK_BERLIN)
                .when()
                .checkOut()
                .withCartContaining(HP_15, 2)
                .withCartContaining(DELL_XPS, 1)
                .then()
                .shouldSucceed()
                .checkout()
                .hasReviewLines(HP_15, DELL_XPS)
                .hasReviewLine(HP_15, 2, '$1,399.98')
                .hasReviewLine(DELL_XPS, 1, '$1,299.99')
                .hasReviewSubtotal('$2,699.97')
                .saysDiscountAndTaxAreWorkedOutPerOrder();
        });

        test('shouldPriceEachOrderFromTheErpWhenItIsPlaced', criteria('UC-009.5'), async ({ scenario }) => {
            await givenTheCatalogAndSavedAddress(scenario, JOHN_SMITH_NEW_YORK)
                .when()
                .checkOut()
                .withCartContaining(HP_15, 1)
                .withErpPriceChanged(HP_15, '649.99')
                .withOrderPlaced()
                .then()
                .shouldSucceed()
                .orderFor(HP_15)
                .hasUnitPrice('649.99');
        });

        test('shouldConfirmEveryOrderAndEmptyTheCart', criteria('UC-009.6'), async ({ scenario }) => {
            await givenTheCatalogAndSavedAddress(scenario, ANA_NOVAK_BERLIN)
                .when()
                .checkOut()
                .withCartContaining(HP_15, 2)
                .withCartContaining(DELL_XPS, 1)
                .withOrderPlaced()
                .then()
                .shouldSucceed()
                .checkout()
                .thanksForTheOrder()
                .confirmsOrdersFor(HP_15, DELL_XPS)
                .confirmsNothingRejected()
                .linksEachOrderToItsDetails()
                .offersViewMyOrdersAndContinueShopping()
                .hasCartLines();
        });
    });
});
