import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';
import { ALL_PRODUCTS, HP_15, givenTheCatalog } from './base/browse-catalog-background.js';

// Isolated: stages the shared ERP stub's product list and takes the shared ERP stub down.
// The messages, the link back to the catalog, the cart and the working site are what the UI shows; the API has none of them.

const NO_SUCH_SKU = 'NO-SUCH-SKU';
const PRODUCT_NOT_FOUND = 'Product not found';
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
        test('shouldShowProductNotFoundForAnUnknownSku', criteria('UC-007.5', 'narrative-2a'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .viewProduct()
                .withSku(NO_SUCH_SKU)
                .then()
                .shouldSucceed()
                .productPage()
                .showsMessage(PRODUCT_NOT_FOUND)
                .offersBackToCatalog();
        });

        test('shouldGoBackToTheCatalogFromProductNotFound', criteria('UC-007.5', 'narrative-2a'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .viewProduct()
                .withSku(NO_SUCH_SKU)
                .withBackToCatalogFollowed()
                .then()
                .shouldSucceed()
                .productPage()
                .catalogShowsProducts(...ALL_PRODUCTS);
        });

        test('shouldTellTheVisitorWhenTheErpIsUnavailable', criteria('UC-007.6', 'narrative-2b'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .and()
                .erp()
                .unavailable()
                .when()
                .viewProduct()
                .withSku(HP_15)
                .then()
                .shouldSucceed()
                .productPage()
                .showsMessage(PRODUCTS_UNAVAILABLE)
                .doesNotOfferAddToCart();
        });

        test(
            'shouldKeepTheRestOfTheSiteWorkingWhenTheErpIsUnavailable',
            criteria('UC-007.6', 'narrative-2b'),
            async ({ scenario }) => {
                await givenTheCatalog(scenario)
                    .and()
                    .loggedInAsCustomer()
                    .erp()
                    .unavailable()
                    .when()
                    .viewProduct()
                    .withSku(HP_15)
                    .then()
                    .shouldSucceed()
                    .productPage()
                    .showsMessage(PRODUCTS_UNAVAILABLE)
                    .canOpenMyOrders();
            },
        );

        test.each(invalidQuantityCases)(
            'shouldNotAddAQuantityThatIsNotAWholeNumberFrom1To99_$label',
            criteria('UC-007.7', 'narrative-5a'),
            async ({ scenario, quantity }) => {
                await givenTheCatalog(scenario)
                    .when()
                    .viewProduct()
                    .withSku(HP_15)
                    .withAddedToCart(quantity)
                    .then()
                    .shouldSucceed()
                    .productPage()
                    .showsMessage(QUANTITY_ERROR)
                    .cartIsEmpty();
            },
        );
    });
});
