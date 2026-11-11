package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseViewProductTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.ValueSource;

// The messages, the link back to the catalog, the cart and the working site are what the UI shows; the API has none of them.
@Isolated("stages the shared ERP stub's product list and takes the shared ERP stub down")
class ViewProductNegativeIsolatedTest extends BaseViewProductTest {
    private static final String PRODUCT_NOT_FOUND = "Product not found";
    private static final String PRODUCTS_UNAVAILABLE = "Products are temporarily unavailable.";
    private static final String QUANTITY_ERROR = "Quantity must be a whole number from 1 to 99.";

    @Tag("UC-007.5")
    @Tag("narrative-2a")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowProductNotFoundForAnUnknownSku() {
        givenTheCatalog()
                .when().viewProduct()
                    .withSku(NO_SUCH_SKU)
                .then().shouldSucceed()
                    .productPage()
                        .showsMessage(PRODUCT_NOT_FOUND)
                        .offersBackToCatalog();
    }

    @Tag("UC-007.5")
    @Tag("narrative-2a")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldGoBackToTheCatalogFromProductNotFound() {
        givenTheCatalog()
                .when().viewProduct()
                    .withSku(NO_SUCH_SKU)
                    .withBackToCatalogFollowed()
                .then().shouldSucceed()
                    .productPage()
                        .catalogShowsProducts(ALL_PRODUCTS);
    }

    @Tag("UC-007.6")
    @Tag("narrative-2b")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldTellTheVisitorWhenTheErpIsUnavailable() {
        givenTheCatalog()
                .and().erp()
                    .unavailable()
                .when().viewProduct()
                    .withSku(HP_15)
                .then().shouldSucceed()
                    .productPage()
                        .showsMessage(PRODUCTS_UNAVAILABLE)
                        .doesNotOfferAddToCart();
    }

    @Tag("UC-007.6")
    @Tag("narrative-2b")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldKeepTheRestOfTheSiteWorkingWhenTheErpIsUnavailable() {
        givenTheCatalog()
                .and().loggedInAsCustomer()
                .erp()
                    .unavailable()
                .when().viewProduct()
                    .withSku(HP_15)
                .then().shouldSucceed()
                    .productPage()
                        .showsMessage(PRODUCTS_UNAVAILABLE)
                        .canOpenMyOrders();
    }

    @Tag("UC-007.7")
    @Tag("narrative-5a")
    @TestTemplate
    @Channel(ChannelType.UI)
    @ValueSource(strings = {"0", "-1", "1.5", "abc", "100"})
    void shouldNotAddAQuantityThatIsNotAWholeNumberFrom1To99(String quantity) {
        givenTheCatalog()
                .when().viewProduct()
                    .withSku(HP_15)
                    .withAddedToCart(quantity)
                .then().shouldSucceed()
                    .productPage()
                        .showsMessage(QUANTITY_ERROR)
                        .cartIsEmpty();
    }
}
