import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';
import { HP_15, givenTheCatalog } from './base/browse-catalog-background.js';

// Isolated: stages the shared ERP stub's product list and takes the shared ERP stub down.
// The cart lives in the browser and only the UI shows it and its messages; there is no cart API.

const CART_IS_EMPTY = 'Your cart is empty.';
const PRODUCTS_UNAVAILABLE = 'Products are temporarily unavailable.';
const QUANTITY_ERROR = 'Quantity must be a whole number from 1 to 99.';

const invalidQuantityCases = [
    { label: '0', quantity: '0' },
    { label: 'minus1', quantity: '-1' },
    { label: '1_5', quantity: '1.5' },
    { label: 'abc', quantity: 'abc' },
    { label: '100', quantity: '100' },
];

test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });

    forChannels(ChannelType.UI)(() => {
        test('shouldTellTheVisitorWhenTheCartIsEmpty', criteria('UC-008.9', 'narrative-4a'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .manageCart()
                .then()
                .shouldSucceed()
                .cart()
                .showsMessage(CART_IS_EMPTY)
                .offersStartShopping();
        });

        test('shouldShowTheEmptyCartAfterRemovingTheLastLine', criteria('UC-008.9', 'narrative-4a'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .manageCart()
                .withCartContaining(HP_15, 1)
                .withLineRemoved(HP_15)
                .then()
                .shouldSucceed()
                .cart()
                .showsMessage(CART_IS_EMPTY)
                .hasNoCartBadge();
        });

        test(
            'shouldTellTheVisitorWhenTheErpIsUnavailableAndKeepTheCart',
            criteria('UC-008.10', 'narrative-4b'),
            async ({ scenario }) => {
                await givenTheCatalog(scenario)
                    .when()
                    .manageCart()
                    .withCartContaining(HP_15, 2)
                    .withErpUnavailable()
                    .then()
                    .shouldSucceed()
                    .cart()
                    .showsMessage(PRODUCTS_UNAVAILABLE)
                    .hasCartBadge(2);
            },
        );

        test('shouldShowTheCartAgainOnceTheErpIsBack', criteria('UC-008.10', 'narrative-4b'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .manageCart()
                .withCartContaining(HP_15, 2)
                .withCartViewedWhileErpUnavailable()
                .then()
                .shouldSucceed()
                .cart()
                .hasLineQuantity(HP_15, 2)
                .hasLineTotal(HP_15, '$1,399.98');
        });

        test.each(invalidQuantityCases)(
            'shouldKeepThePreviousQuantityWhenTheNewOneIsNotAWholeNumberFrom1To99_$label',
            criteria('UC-008.11', 'narrative-5a'),
            async ({ scenario, quantity }) => {
                await givenTheCatalog(scenario)
                    .when()
                    .manageCart()
                    .withCartContaining(HP_15, 2)
                    .withQuantityChanged(HP_15, quantity)
                    .then()
                    .shouldSucceed()
                    .cart()
                    .showsMessage(QUANTITY_ERROR)
                    .hasLineQuantity(HP_15, 2);
            },
        );
    });
});
