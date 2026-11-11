package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseManageCartTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.ValueSource;

// The cart lives in the browser and only the UI shows it and its messages; there is no cart API.
@Isolated("stages the shared ERP stub's product list and takes the shared ERP stub down")
class ManageCartNegativeIsolatedTest extends BaseManageCartTest {
    private static final String CART_IS_EMPTY = "Your cart is empty.";
    private static final String PRODUCTS_UNAVAILABLE = "Products are temporarily unavailable.";
    private static final String QUANTITY_ERROR = "Quantity must be a whole number from 1 to 99.";

    @Tag("UC-008.9")
    @Tag("narrative-4a")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldTellTheVisitorWhenTheCartIsEmpty() {
        givenTheCatalog()
                .when().manageCart()
                .then().shouldSucceed()
                    .cart()
                        .showsMessage(CART_IS_EMPTY)
                        .offersStartShopping();
    }

    @Tag("UC-008.9")
    @Tag("narrative-4a")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowTheEmptyCartAfterRemovingTheLastLine() {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(HP_15, 1)
                    .withLineRemoved(HP_15)
                .then().shouldSucceed()
                    .cart()
                        .showsMessage(CART_IS_EMPTY)
                        .hasNoCartBadge();
    }

    @Tag("UC-008.10")
    @Tag("narrative-4b")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldTellTheVisitorWhenTheErpIsUnavailableAndKeepTheCart() {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(HP_15, 2)
                    .withErpUnavailable()
                .then().shouldSucceed()
                    .cart()
                        .showsMessage(PRODUCTS_UNAVAILABLE)
                        .hasCartBadge(2);
    }

    @Tag("UC-008.10")
    @Tag("narrative-4b")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowTheCartAgainOnceTheErpIsBack() {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(HP_15, 2)
                    .withCartViewedWhileErpUnavailable()
                .then().shouldSucceed()
                    .cart()
                        .hasLineQuantity(HP_15, 2)
                        .hasLineTotal(HP_15, "$1,399.98");
    }

    @Tag("UC-008.11")
    @Tag("narrative-5a")
    @TestTemplate
    @Channel(ChannelType.UI)
    @ValueSource(strings = {"0", "-1", "1.5", "abc", "100"})
    void shouldKeepThePreviousQuantityWhenTheNewOneIsNotAWholeNumberFrom1To99(String quantity) {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(HP_15, 2)
                    .withQuantityChanged(HP_15, quantity)
                .then().shouldSucceed()
                    .cart()
                        .showsMessage(QUANTITY_ERROR)
                        .hasLineQuantity(HP_15, 2);
    }
}
