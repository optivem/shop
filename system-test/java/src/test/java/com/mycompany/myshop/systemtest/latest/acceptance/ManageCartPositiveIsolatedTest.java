package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseManageCartTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

// The cart lives in the browser (server session, cookie or local storage) and only the UI shows it; there is no cart API.
@Isolated("stages the shared ERP stub's product list, which every cart reads")
class ManageCartPositiveIsolatedTest extends BaseManageCartTest {
    private static final String ADDED_TO_CART = "Added to cart";

    @Tag("UC-008.1")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowTheCartsTotalQuantityInTheHeader() {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(HP_15, 2)
                    .withCartContaining(DELL_XPS, 1)
                    .withCatalogBrowsed()
                .then().shouldSucceed()
                    .cart()
                        .hasCartBadge(3);
    }

    @Tag("UC-008.1")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldHideTheCartBadgeWhenTheCartIsEmpty() {
        givenTheCatalog()
                .when().manageCart()
                    .withCatalogBrowsed()
                .then().shouldSucceed()
                    .cart()
                        .hasNoCartBadge();
    }

    @Tag("UC-008.2")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldAddQuantity1FromTheCatalog() {
        givenTheCatalog()
                .when().manageCart()
                    .withAddedFromCatalog(LOGI_MX3S)
                .then().shouldSucceed()
                    .cart()
                        .showsMessage(ADDED_TO_CART)
                        .hasLines(LOGI_MX3S)
                        .hasLineQuantity(LOGI_MX3S, 1)
                        .hasCartBadge(1);
    }

    @Tag("UC-008.2")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldRaiseTheQuantityOfAProductAlreadyInTheCart() {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(LOGI_MX3S, 1)
                    .withAddedFromCatalog(LOGI_MX3S)
                .then().shouldSucceed()
                    .cart()
                        .hasLines(LOGI_MX3S)
                        .hasLineQuantity(LOGI_MX3S, 2)
                        .hasItemCount(1);
    }

    private static Stream<Arguments> provideCapCases() {
        return Stream.of(
                Arguments.of(97, "2", 99),
                Arguments.of(98, "5", 99),
                Arguments.of(99, "1", 99)
        );
    }

    @Tag("UC-008.3")
    @TestTemplate
    @Channel(ChannelType.UI)
    @MethodSource("provideCapCases")
    void shouldCapTheQuantityOfALineAt99WhenAdding(int before, String added, int after) {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(HP_15, before)
                    .withAddedFromProductPage(HP_15, added)
                .then().shouldSucceed()
                    .cart()
                        .hasLines(HP_15)
                        .hasLineQuantity(HP_15, after);
    }

    @Tag("UC-008.4")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowEachCartLineWithItsTitleUnitPriceQuantityAndLineTotal() {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(HP_15, 2)
                    .withCartContaining(DELL_XPS, 1)
                .then().shouldSucceed()
                    .cart()
                        .hasLines(HP_15, DELL_XPS)
                        .hasLine(HP_15, "HP Laptop 15", "$699.99", 2, "$1,399.98")
                        .hasLine(DELL_XPS, "Dell XPS 13", "$1,299.99", 1, "$1,299.99")
                        .hasItemCount(2);
    }

    @Tag("UC-008.4")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldListTheLinesInTheOrderTheirProductsWereFirstAdded() {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(LG_27UL500, 1)
                    .withCartContaining(HP_15, 1)
                    .withCartContaining(LG_27UL500, 1)
                .then().shouldSucceed()
                    .cart()
                        .hasLines(LG_27UL500, HP_15);
    }

    @Tag("UC-008.5")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowTheSubtotalAndLeaveTaxAndDiscountsToCheckout() {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(HP_15, 2)
                    .withCartContaining(DELL_XPS, 1)
                .then().shouldSucceed()
                    .cart()
                        .hasSubtotal("$2,699.97")
                        .saysTaxAndDiscountsAreCalculatedAtCheckout();
    }

    @Tag("UC-008.5")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowTheCurrentCatalogPrice() {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(HP_15, 2)
                    .withErpPriceChanged(HP_15, "649.99")
                .then().shouldSucceed()
                    .cart()
                        .hasLinePrices(HP_15, "$649.99", "$1,299.98")
                        .hasSubtotal("$1,299.98");
    }

    @Tag("UC-008.6")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldUpdateTheLineTotalSubtotalAndBadgeWhenTheQuantityChanges() {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(HP_15, 2)
                    .withCartContaining(DELL_XPS, 1)
                    .withQuantityChanged(HP_15, "3")
                .then().shouldSucceed()
                    .cart()
                        .hasLineQuantity(HP_15, 3)
                        .hasLineTotal(HP_15, "$2,099.97")
                        .hasSubtotal("$3,399.96")
                        .hasCartBadge(4);
    }

    private static Stream<Arguments> provideStepperCases() {
        return Stream.of(
                Arguments.of(1, true, 2),
                Arguments.of(2, false, 1),
                Arguments.of(1, false, 1),
                Arguments.of(99, true, 99)
        );
    }

    @Tag("UC-008.6")
    @TestTemplate
    @Channel(ChannelType.UI)
    @MethodSource("provideStepperCases")
    void shouldKeepTheQuantityBetween1And99WhenUsingTheStepper(int before, boolean increases, int after) {
        var manageCart = givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(HP_15, before);
        if (increases) {
            manageCart.withQuantityIncreased(HP_15);
        } else {
            manageCart.withQuantityDecreased(HP_15);
        }
        manageCart
                .then().shouldSucceed()
                    .cart()
                        .hasLineQuantity(HP_15, after);
    }

    @Tag("UC-008.7")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldRemoveALineFromTheCart() {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(HP_15, 2)
                    .withCartContaining(DELL_XPS, 1)
                    .withLineRemoved(HP_15)
                .then().shouldSucceed()
                    .cart()
                        .hasLines(DELL_XPS)
                        .hasSubtotal("$1,299.99")
                        .hasCartBadge(1);
    }

    @Tag("UC-008.8")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldKeepTheCartWithoutSigningIn() {
        givenTheCatalog()
                .and().notLoggedIn()
                .when().manageCart()
                    .withAddedFromCatalog(HP_15)
                .then().shouldSucceed()
                    .cart()
                        .didNotAskToSignIn()
                        .hasLines(HP_15);
    }

    // The monoliths keep the cart in the server session or a cookie, React in local storage: a visit later in the
    // same browser keeps all of them.
    @Tag("UC-008.8")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldKeepTheCartAcrossVisits() {
        givenTheCatalog()
                .when().manageCart()
                    .withCartContaining(HP_15, 2)
                    .withSiteLeftAndRevisited()
                .then().shouldSucceed()
                    .cart()
                        .hasLines(HP_15)
                        .hasLineQuantity(HP_15, 2);
    }

    @Tag("UC-008.8")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldKeepTheCartWhenTheVisitorSignsIn() {
        givenTheCatalog()
                .and().notLoggedIn()
                .when().manageCart()
                    .withCartContaining(HP_15, 2)
                    .withCartContaining(DELL_XPS, 1)
                    .withSignedInAsCustomer()
                .then().shouldSucceed()
                    .cart()
                        .hasLines(HP_15, DELL_XPS)
                        .hasLineQuantity(HP_15, 2)
                        .hasLineQuantity(DELL_XPS, 1)
                        .hasCartBadge(3);
    }
}
