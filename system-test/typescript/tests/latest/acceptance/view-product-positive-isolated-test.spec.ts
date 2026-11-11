import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';
import { DELL_XPS, HP_15, LENOVO_T14, LOGI_MX3S, givenTheCatalog } from './base/browse-catalog-background.js';

// Isolated: stages the shared ERP stub's product list, which every product page reads.
// UC-007's Background is UC-006's product list and an empty cart (every test starts a fresh session).
// The formatted price, breadcrumbs, quantity stepper and cart exist only in the UI; the API serves the product's fields.

const ADDED_TO_CART = 'Added to cart';

const stepperCases = [
    { label: '1_increases', before: '1', increases: true, after: 2 },
    { label: '2_decreases', before: '2', increases: false, after: 1 },
    { label: '1_decreases', before: '1', increases: false, after: 1 },
    { label: '99_increases', before: '99', increases: true, after: 99 },
];

test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });

    forChannels(ChannelType.UI, ChannelType.API)(() => {
        test('shouldLetAnAnonymousVisitorViewAProductWithoutSigningIn', criteria('UC-007.4'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .and()
                .notLoggedIn()
                .when()
                .viewProduct()
                .withSku(LENOVO_T14)
                .then()
                .shouldSucceed()
                .productPage()
                .hasTitle('Lenovo ThinkPad T14');
        });
    });

    forChannels(ChannelType.UI)(() => {
        test('shouldShowTheDetailsOfTheProduct', criteria('UC-007.1'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .viewProduct()
                .withSku(DELL_XPS)
                .then()
                .shouldSucceed()
                .productPage()
                .hasTitle('Dell XPS 13')
                .hasBrand('Dell')
                .hasCategory('Laptops')
                .hasSku(DELL_XPS)
                .hasPrice('$1,299.99')
                .hasDescription('Compact 13-inch ultrabook');
        });

        test('shouldShowTheBreadcrumbTrailOfTheProduct', criteria('UC-007.1'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .viewProduct()
                .withSku(LOGI_MX3S)
                .then()
                .shouldSucceed()
                .productPage()
                .hasBreadcrumbs('Home', 'Accessories', 'Logitech MX Master 3S');
        });

        test('shouldOpenTheProductFromTheCatalog', criteria('UC-007.1'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .viewProduct()
                .withSku(HP_15)
                .fromCatalog()
                .then()
                .shouldSucceed()
                .productPage()
                .hasTitle('HP Laptop 15')
                .hasPrice('$699.99');
        });

        test('shouldStartWithQuantity1', criteria('UC-007.2'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .viewProduct()
                .withSku(HP_15)
                .then()
                .shouldSucceed()
                .productPage()
                .hasQuantity(1);
        });

        test.each(stepperCases)(
            'shouldKeepTheQuantityBetween1And99WhenUsingTheStepper_$label',
            criteria('UC-007.2'),
            async ({ scenario, before, increases, after }) => {
                const viewProduct = givenTheCatalog(scenario).when().viewProduct().withSku(HP_15).withQuantity(before);
                const stepped = increases ? viewProduct.withQuantityIncreased() : viewProduct.withQuantityDecreased();
                await stepped.then().shouldSucceed().productPage().hasQuantity(after);
            },
        );

        test('shouldAddTheChosenQuantityToTheCart', criteria('UC-007.3'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .viewProduct()
                .withSku(HP_15)
                .withAddedToCart('3')
                .then()
                .shouldSucceed()
                .productPage()
                .showsMessage(ADDED_TO_CART)
                .cartContains(HP_15, 3)
                .hasCartBadge(3)
                .isOnPageOfProduct(HP_15);
        });

        test('shouldAddToAProductAlreadyInTheCart', criteria('UC-007.3'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .viewProduct()
                .withCartContaining(HP_15, 1)
                .withSku(HP_15)
                .withAddedToCart('2')
                .then()
                .shouldSucceed()
                .productPage()
                .cartContains(HP_15, 3);
        });

        test('shouldShowASignedInCustomerTheSameProductPage', criteria('UC-007.4'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .and()
                .loggedInAsCustomer()
                .when()
                .viewProduct()
                .withSku(LENOVO_T14)
                .then()
                .shouldSucceed()
                .productPage()
                .hasTitle('Lenovo ThinkPad T14')
                .hasBrand('Lenovo')
                .hasCategory('Laptops')
                .hasSku(LENOVO_T14)
                .hasPrice('$999.99');
        });
    });
});
