import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';
import { DELL_XPS, HP_15, LG_27UL500, LOGI_MX3S, givenTheCatalog } from './base/browse-catalog-background.js';

// Isolated: stages the shared ERP stub's product list, which every cart reads.
// UC-008's Background is UC-006's product list and an empty cart (every test starts a fresh session).
// The cart lives in the browser (server session, cookie or local storage) and only the UI shows it; there is no cart API.

const ADDED_TO_CART = 'Added to cart';

const capCases = [
    { label: '97_2_99', before: 97, added: '2', after: 99 },
    { label: '98_5_99', before: 98, added: '5', after: 99 },
    { label: '99_1_99', before: 99, added: '1', after: 99 },
];

const stepperCases = [
    { label: '1_increases', before: 1, increases: true, after: 2 },
    { label: '2_decreases', before: 2, increases: false, after: 1 },
    { label: '1_decreases', before: 1, increases: false, after: 1 },
    { label: '99_increases', before: 99, increases: true, after: 99 },
];

test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });

    forChannels(ChannelType.UI)(() => {
        test('shouldShowTheCartsTotalQuantityInTheHeader', criteria('UC-008.1'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .manageCart()
                .withCartContaining(HP_15, 2)
                .withCartContaining(DELL_XPS, 1)
                .withCatalogBrowsed()
                .then()
                .shouldSucceed()
                .cart()
                .hasCartBadge(3);
        });

        test('shouldHideTheCartBadgeWhenTheCartIsEmpty', criteria('UC-008.1'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .manageCart()
                .withCatalogBrowsed()
                .then()
                .shouldSucceed()
                .cart()
                .hasNoCartBadge();
        });

        test('shouldAddQuantity1FromTheCatalog', criteria('UC-008.2'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .manageCart()
                .withAddedFromCatalog(LOGI_MX3S)
                .then()
                .shouldSucceed()
                .cart()
                .showsMessage(ADDED_TO_CART)
                .hasLines(LOGI_MX3S)
                .hasLineQuantity(LOGI_MX3S, 1)
                .hasCartBadge(1);
        });

        test('shouldRaiseTheQuantityOfAProductAlreadyInTheCart', criteria('UC-008.2'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .manageCart()
                .withCartContaining(LOGI_MX3S, 1)
                .withAddedFromCatalog(LOGI_MX3S)
                .then()
                .shouldSucceed()
                .cart()
                .hasLines(LOGI_MX3S)
                .hasLineQuantity(LOGI_MX3S, 2)
                .hasItemCount(1);
        });

        test.each(capCases)(
            'shouldCapTheQuantityOfALineAt99WhenAdding_$label',
            criteria('UC-008.3'),
            async ({ scenario, before, added, after }) => {
                await givenTheCatalog(scenario)
                    .when()
                    .manageCart()
                    .withCartContaining(HP_15, before)
                    .withAddedFromProductPage(HP_15, added)
                    .then()
                    .shouldSucceed()
                    .cart()
                    .hasLines(HP_15)
                    .hasLineQuantity(HP_15, after);
            },
        );

        test(
            'shouldShowEachCartLineWithItsTitleUnitPriceQuantityAndLineTotal',
            criteria('UC-008.4'),
            async ({ scenario }) => {
                await givenTheCatalog(scenario)
                    .when()
                    .manageCart()
                    .withCartContaining(HP_15, 2)
                    .withCartContaining(DELL_XPS, 1)
                    .then()
                    .shouldSucceed()
                    .cart()
                    .hasLines(HP_15, DELL_XPS)
                    .hasLine(HP_15, 'HP Laptop 15', '$699.99', 2, '$1,399.98')
                    .hasLine(DELL_XPS, 'Dell XPS 13', '$1,299.99', 1, '$1,299.99')
                    .hasItemCount(2);
            },
        );

        test('shouldListTheLinesInTheOrderTheirProductsWereFirstAdded', criteria('UC-008.4'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .manageCart()
                .withCartContaining(LG_27UL500, 1)
                .withCartContaining(HP_15, 1)
                .withCartContaining(LG_27UL500, 1)
                .then()
                .shouldSucceed()
                .cart()
                .hasLines(LG_27UL500, HP_15);
        });

        test('shouldShowTheSubtotalAndLeaveTaxAndDiscountsToCheckout', criteria('UC-008.5'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .manageCart()
                .withCartContaining(HP_15, 2)
                .withCartContaining(DELL_XPS, 1)
                .then()
                .shouldSucceed()
                .cart()
                .hasSubtotal('$2,699.97')
                .saysTaxAndDiscountsAreCalculatedAtCheckout();
        });

        test('shouldShowTheCurrentCatalogPrice', criteria('UC-008.5'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .manageCart()
                .withCartContaining(HP_15, 2)
                .withErpPriceChanged(HP_15, '649.99')
                .then()
                .shouldSucceed()
                .cart()
                .hasLinePrices(HP_15, '$649.99', '$1,299.98')
                .hasSubtotal('$1,299.98');
        });

        test(
            'shouldUpdateTheLineTotalSubtotalAndBadgeWhenTheQuantityChanges',
            criteria('UC-008.6'),
            async ({ scenario }) => {
                await givenTheCatalog(scenario)
                    .when()
                    .manageCart()
                    .withCartContaining(HP_15, 2)
                    .withCartContaining(DELL_XPS, 1)
                    .withQuantityChanged(HP_15, '3')
                    .then()
                    .shouldSucceed()
                    .cart()
                    .hasLineQuantity(HP_15, 3)
                    .hasLineTotal(HP_15, '$2,099.97')
                    .hasSubtotal('$3,399.96')
                    .hasCartBadge(4);
            },
        );

        test.each(stepperCases)(
            'shouldKeepTheQuantityBetween1And99WhenUsingTheStepper_$label',
            criteria('UC-008.6'),
            async ({ scenario, before, increases, after }) => {
                const manageCart = givenTheCatalog(scenario).when().manageCart().withCartContaining(HP_15, before);
                const stepped = increases ? manageCart.withQuantityIncreased(HP_15) : manageCart.withQuantityDecreased(HP_15);
                await stepped.then().shouldSucceed().cart().hasLineQuantity(HP_15, after);
            },
        );

        test('shouldRemoveALineFromTheCart', criteria('UC-008.7'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .manageCart()
                .withCartContaining(HP_15, 2)
                .withCartContaining(DELL_XPS, 1)
                .withLineRemoved(HP_15)
                .then()
                .shouldSucceed()
                .cart()
                .hasLines(DELL_XPS)
                .hasSubtotal('$1,299.99')
                .hasCartBadge(1);
        });

        test('shouldKeepTheCartWithoutSigningIn', criteria('UC-008.8'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .and()
                .notLoggedIn()
                .when()
                .manageCart()
                .withAddedFromCatalog(HP_15)
                .then()
                .shouldSucceed()
                .cart()
                .didNotAskToSignIn()
                .hasLines(HP_15);
        });

        // The monoliths keep the cart in the server session or a cookie, React in local storage: a visit later in the
        // same browser keeps all of them.
        test('shouldKeepTheCartAcrossVisits', criteria('UC-008.8'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .manageCart()
                .withCartContaining(HP_15, 2)
                .withSiteLeftAndRevisited()
                .then()
                .shouldSucceed()
                .cart()
                .hasLines(HP_15)
                .hasLineQuantity(HP_15, 2);
        });

        test('shouldKeepTheCartWhenTheVisitorSignsIn', criteria('UC-008.8'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .and()
                .notLoggedIn()
                .when()
                .manageCart()
                .withCartContaining(HP_15, 2)
                .withCartContaining(DELL_XPS, 1)
                .withSignedInAsCustomer()
                .then()
                .shouldSucceed()
                .cart()
                .hasLines(HP_15, DELL_XPS)
                .hasLineQuantity(HP_15, 2)
                .hasLineQuantity(DELL_XPS, 1)
                .hasCartBadge(3);
        });
    });
});
