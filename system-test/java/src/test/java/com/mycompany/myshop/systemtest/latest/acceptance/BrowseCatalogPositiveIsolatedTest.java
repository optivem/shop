package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseBrowseCatalogTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

// Filtering, search and sort exist only in the UI; the API serves the unfiltered list.
@Isolated("stages the shared ERP stub's product list, which every catalog page reads")
class BrowseCatalogPositiveIsolatedTest extends BaseBrowseCatalogTest {
    @Tag("UC-006.1")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldListEveryErpProductInTheErpsOrder() {
        givenTheCatalog()
                .when().browseCatalog()
                .then().shouldSucceed()
                    .catalog()
                        .showsProducts(ALL_PRODUCTS)
                        .hasProductCount(6);
    }

    @Tag("UC-006.1")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowTheTitleBrandAndPriceOfEachProduct() {
        givenTheCatalog()
                .when().browseCatalog()
                .then().shouldSucceed()
                    .catalog()
                        .showsProduct(DELL_XPS, "Dell XPS 13", "Dell", "$1,299.99")
                        .showsProduct(LOGI_MX3S, "Logitech MX Master 3S", "Logitech", "$99.99");
    }

    @Tag("UC-006.2")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldOfferTheCategoriesOfTheCatalogsProducts() {
        givenTheCatalog()
                .when().browseCatalog()
                .then().shouldSucceed()
                    .catalog()
                        .offersCategories("All", "Laptops", "Monitors", "Accessories")
                        .hasCategorySelected("All");
    }

    @Tag("UC-006.2")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowOnlyTheProductsInTheChosenCategory() {
        givenTheCatalog()
                .when().browseCatalog()
                    .withCategory("Monitors")
                .then().shouldSucceed()
                    .catalog()
                        .showsProducts(DELL_U2723QE, LG_27UL500)
                        .hasProductCount(2);
    }

    @Tag("UC-006.2")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldFilterByCategoryFromTheHeaderLink() {
        givenTheCatalog()
                .when().browseCatalog()
                    .withHeaderCategoryLink("Laptops")
                .then().shouldSucceed()
                    .catalog()
                        .showsProducts(HP_15, DELL_XPS, LENOVO_T14)
                        .hasCategorySelected("Laptops");
    }

    @Tag("UC-006.3")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldOfferTheBrandsOfTheCatalogsProducts() {
        givenTheCatalog()
                .when().browseCatalog()
                .then().shouldSucceed()
                    .catalog()
                        .offersBrands("HP", "Dell", "Lenovo", "LG", "Logitech")
                        .hasNoBrandTicked();
    }

    private static Stream<Arguments> provideBrandFilters() {
        return Stream.of(
                Arguments.of(new String[]{"Dell"}, new String[]{DELL_XPS, DELL_U2723QE}),
                Arguments.of(new String[]{"Dell", "LG"}, new String[]{DELL_XPS, DELL_U2723QE, LG_27UL500}),
                Arguments.of(new String[]{}, ALL_PRODUCTS)
        );
    }

    @Tag("UC-006.3")
    @TestTemplate
    @Channel(ChannelType.UI)
    @MethodSource("provideBrandFilters")
    void shouldShowOnlyTheProductsOfTheTickedBrands(String[] brands, String[] expectedProducts) {
        givenTheCatalog()
                .when().browseCatalog()
                    .withBrands(brands)
                .then().shouldSucceed()
                    .catalog()
                        .showsProducts(expectedProducts);
    }

    private static Stream<Arguments> provideSearchTerms() {
        return Stream.of(
                Arguments.of("thinkpad", new String[]{LENOVO_T14}),
                Arguments.of("LOGITECH", new String[]{LOGI_MX3S}),
                Arguments.of("wireless", new String[]{LOGI_MX3S}),
                Arguments.of("4k", new String[]{DELL_U2723QE, LG_27UL500}),
                Arguments.of("", ALL_PRODUCTS)
        );
    }

    @Tag("UC-006.4")
    @TestTemplate
    @Channel(ChannelType.UI)
    @MethodSource("provideSearchTerms")
    void shouldFindProductsByTitleBrandOrDescriptionIgnoringCase(String term, String[] expectedProducts) {
        givenTheCatalog()
                .when().browseCatalog()
                    .withSearch(term)
                .then().shouldSucceed()
                    .catalog()
                        .showsProducts(expectedProducts);
    }

    @Tag("UC-006.5")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowOnlyProductsThatMatchEveryFilter() {
        givenTheCatalog()
                .when().browseCatalog()
                    .withCategory("Laptops")
                    .withBrands("Dell")
                .then().shouldSucceed()
                    .catalog()
                        .showsProducts(DELL_XPS)
                        .hasProductCount(1);
    }

    private static Stream<Arguments> provideSortOrders() {
        return Stream.of(
                Arguments.of(FEATURED, ALL_PRODUCTS),
                Arguments.of(PRICE_LOW_TO_HIGH, new String[]{LOGI_MX3S, LG_27UL500, DELL_U2723QE, HP_15, LENOVO_T14, DELL_XPS}),
                Arguments.of(PRICE_HIGH_TO_LOW, new String[]{DELL_XPS, LENOVO_T14, HP_15, DELL_U2723QE, LG_27UL500, LOGI_MX3S}),
                Arguments.of(NAME_A_TO_Z, new String[]{DELL_U2723QE, DELL_XPS, HP_15, LENOVO_T14, LG_27UL500, LOGI_MX3S})
        );
    }

    @Tag("UC-006.6")
    @TestTemplate
    @Channel(ChannelType.UI)
    @MethodSource("provideSortOrders")
    void shouldSortTheProducts(String sort, String[] expectedProducts) {
        givenTheCatalog()
                .when().browseCatalog()
                    .withSort(sort)
                .then().shouldSucceed()
                    .catalog()
                        .showsProducts(expectedProducts);
    }

    @Tag("UC-006.6")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldSortOnlyTheFilteredProducts() {
        givenTheCatalog()
                .when().browseCatalog()
                    .withCategory("Laptops")
                    .withSort(PRICE_HIGH_TO_LOW)
                .then().shouldSucceed()
                    .catalog()
                        .showsProducts(DELL_XPS, LENOVO_T14, HP_15);
    }

    @Tag("UC-006.7")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowTheSameFilteredViewWhenThePageAddressIsOpenedAgain() {
        givenTheCatalog()
                .when().browseCatalog()
                    .withCategory("Laptops")
                    .withSort(PRICE_HIGH_TO_LOW)
                    .withPageAddressReopened()
                .then().shouldSucceed()
                    .catalog()
                        .showsProducts(DELL_XPS, LENOVO_T14, HP_15)
                        .hasCategorySelected("Laptops")
                        .hasSort(PRICE_HIGH_TO_LOW);
    }

    @Tag("UC-006.8")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldLetAnAnonymousVisitorBrowseWithoutSigningIn() {
        givenTheCatalog()
                .and().notLoggedIn()
                .when().browseCatalog()
                .then().shouldSucceed()
                    .catalog()
                        .hasProductCount(6);
    }

    @Tag("UC-006.8")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldShowASignedInCustomerTheSameCatalog() {
        givenTheCatalog()
                .and().loggedInAsCustomer()
                .when().browseCatalog()
                .then().shouldSucceed()
                    .catalog()
                        .showsProducts(ALL_PRODUCTS);
    }
}
