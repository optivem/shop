package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseBrowseCatalogTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

// The messages, the "Clear filters" offer and the working site are what the UI shows; the API has none of them.
@Isolated("stages the shared ERP stub's product list and takes the shared ERP stub down")
class BrowseCatalogNegativeIsolatedTest extends BaseBrowseCatalogTest {
    private static final String NO_MATCH = "No products match your search.";
    private static final String PRODUCTS_UNAVAILABLE = "Products are temporarily unavailable.";

    @Tag("UC-006.9")
    @Tag("narrative-5a")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldTellTheVisitorWhenNoProductMatches() {
        givenTheCatalog()
                .when().browseCatalog()
                    .withSearch("printer")
                .then().shouldSucceed()
                    .catalog()
                        .showsNoProducts()
                        .showsMessage(NO_MATCH)
                        .offersToClearFilters();
    }

    @Tag("UC-006.9")
    @Tag("narrative-5a")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowTheFullCatalogAfterClearingTheFilters() {
        givenTheCatalog()
                .when().browseCatalog()
                    .withSearch("printer")
                    .withFiltersCleared()
                .then().shouldSucceed()
                    .catalog()
                        .showsProducts(ALL_PRODUCTS)
                        .hasCategorySelected("All")
                        .hasSort(FEATURED);
    }

    @Tag("UC-006.10")
    @Tag("narrative-2a")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldTellTheVisitorWhenTheErpIsUnavailable() {
        givenTheCatalog()
                .and().erp()
                    .unavailable()
                .when().browseCatalog()
                .then().shouldSucceed()
                    .catalog()
                        .showsNoProducts()
                        .showsMessage(PRODUCTS_UNAVAILABLE);
    }

    @Tag("UC-006.10")
    @Tag("narrative-2a")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldKeepTheRestOfTheSiteWorkingWhenTheErpIsUnavailable() {
        givenTheCatalog()
                .and().loggedInAsCustomer()
                .erp()
                    .unavailable()
                .when().browseCatalog()
                .then().shouldSucceed()
                    .catalog()
                        .showsMessage(PRODUCTS_UNAVAILABLE)
                        .canOpenMyOrders();
    }
}
