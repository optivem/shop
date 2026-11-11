package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseViewProductTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

// The formatted price, breadcrumbs, quantity stepper and cart exist only in the UI; the API serves the product's fields.
@Isolated("stages the shared ERP stub's product list, which every product page reads")
class ViewProductPositiveIsolatedTest extends BaseViewProductTest {
    private static final String ADDED_TO_CART = "Added to cart";

    @Tag("UC-007.1")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowTheDetailsOfTheProduct() {
        givenTheCatalog()
                .when().viewProduct()
                    .withSku(DELL_XPS)
                .then().shouldSucceed()
                    .productPage()
                        .hasTitle("Dell XPS 13")
                        .hasBrand("Dell")
                        .hasCategory("Laptops")
                        .hasSku(DELL_XPS)
                        .hasPrice("$1,299.99")
                        .hasDescription("Compact 13-inch ultrabook");
    }

    @Tag("UC-007.1")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowTheBreadcrumbTrailOfTheProduct() {
        givenTheCatalog()
                .when().viewProduct()
                    .withSku(LOGI_MX3S)
                .then().shouldSucceed()
                    .productPage()
                        .hasBreadcrumbs("Home", "Accessories", "Logitech MX Master 3S");
    }

    @Tag("UC-007.1")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldOpenTheProductFromTheCatalog() {
        givenTheCatalog()
                .when().viewProduct()
                    .withSku(HP_15)
                    .fromCatalog()
                .then().shouldSucceed()
                    .productPage()
                        .hasTitle("HP Laptop 15")
                        .hasPrice("$699.99");
    }

    @Tag("UC-007.2")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldStartWithQuantity1() {
        givenTheCatalog()
                .when().viewProduct()
                    .withSku(HP_15)
                .then().shouldSucceed()
                    .productPage()
                        .hasQuantity(1);
    }

    private static Stream<Arguments> provideStepperCases() {
        return Stream.of(
                Arguments.of("1", true, 2),
                Arguments.of("2", false, 1),
                Arguments.of("1", false, 1),
                Arguments.of("99", true, 99)
        );
    }

    @Tag("UC-007.2")
    @TestTemplate
    @Channel(ChannelType.UI)
    @MethodSource("provideStepperCases")
    void shouldKeepTheQuantityBetween1And99WhenUsingTheStepper(String before, boolean increases, int after) {
        var viewProduct = givenTheCatalog()
                .when().viewProduct()
                    .withSku(HP_15)
                    .withQuantity(before);
        if (increases) {
            viewProduct.withQuantityIncreased();
        } else {
            viewProduct.withQuantityDecreased();
        }
        viewProduct
                .then().shouldSucceed()
                    .productPage()
                        .hasQuantity(after);
    }

    @Tag("UC-007.3")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldAddTheChosenQuantityToTheCart() {
        givenTheCatalog()
                .when().viewProduct()
                    .withSku(HP_15)
                    .withAddedToCart("3")
                .then().shouldSucceed()
                    .productPage()
                        .showsMessage(ADDED_TO_CART)
                        .cartContains(HP_15, 3)
                        .hasCartBadge(3)
                        .isOnPageOfProduct(HP_15);
    }

    @Tag("UC-007.3")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldAddToAProductAlreadyInTheCart() {
        givenTheCatalog()
                .when().viewProduct()
                    .withCartContaining(HP_15, 1)
                    .withSku(HP_15)
                    .withAddedToCart("2")
                .then().shouldSucceed()
                    .productPage()
                        .cartContains(HP_15, 3);
    }

    @Tag("UC-007.4")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldLetAnAnonymousVisitorViewAProductWithoutSigningIn() {
        givenTheCatalog()
                .and().notLoggedIn()
                .when().viewProduct()
                    .withSku(LENOVO_T14)
                .then().shouldSucceed()
                    .productPage()
                        .hasTitle("Lenovo ThinkPad T14");
    }

    @Tag("UC-007.4")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowASignedInCustomerTheSameProductPage() {
        givenTheCatalog()
                .and().loggedInAsCustomer()
                .when().viewProduct()
                    .withSku(LENOVO_T14)
                .then().shouldSucceed()
                    .productPage()
                        .hasTitle("Lenovo ThinkPad T14")
                        .hasBrand("Lenovo")
                        .hasCategory("Laptops")
                        .hasSku(LENOVO_T14)
                        .hasPrice("$999.99");
    }
}
