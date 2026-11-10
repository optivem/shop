import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';
import { ALL_PRODUCTS, FEATURED, givenTheCatalog } from './base/browse-catalog-background.js';

// Isolated: stages the shared ERP stub's product list and takes the shared ERP stub down.
// The messages, the "Clear filters" offer and the working site are what the UI shows; the API has none of them.

const NO_MATCH = 'No products match your search.';
const PRODUCTS_UNAVAILABLE = 'Products are temporarily unavailable.';

test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });

    forChannels(ChannelType.UI)(() => {
        test('shouldTellTheVisitorWhenNoProductMatches', criteria('UC-006.9', 'narrative-5a'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .browseCatalog()
                .withSearch('printer')
                .then()
                .shouldSucceed()
                .catalog()
                .showsNoProducts()
                .showsMessage(NO_MATCH)
                .offersToClearFilters();
        });

        test('shouldShowTheFullCatalogAfterClearingTheFilters', criteria('UC-006.9', 'narrative-5a'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .browseCatalog()
                .withSearch('printer')
                .withFiltersCleared()
                .then()
                .shouldSucceed()
                .catalog()
                .showsProducts(...ALL_PRODUCTS)
                .hasCategorySelected('All')
                .hasSort(FEATURED);
        });

        test('shouldTellTheVisitorWhenTheErpIsUnavailable', criteria('UC-006.10', 'narrative-2a'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .and()
                .erp()
                .unavailable()
                .when()
                .browseCatalog()
                .then()
                .shouldSucceed()
                .catalog()
                .showsNoProducts()
                .showsMessage(PRODUCTS_UNAVAILABLE);
        });

        test(
            'shouldKeepTheRestOfTheSiteWorkingWhenTheErpIsUnavailable',
            criteria('UC-006.10', 'narrative-2a'),
            async ({ scenario }) => {
                await givenTheCatalog(scenario)
                    .and()
                    .loggedInAsCustomer()
                    .erp()
                    .unavailable()
                    .when()
                    .browseCatalog()
                    .then()
                    .shouldSucceed()
                    .catalog()
                    .showsMessage(PRODUCTS_UNAVAILABLE)
                    .canOpenMyOrders();
            },
        );
    });
});
