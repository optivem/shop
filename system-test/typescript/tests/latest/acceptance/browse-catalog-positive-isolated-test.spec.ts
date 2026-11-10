import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';
import {
    ALL_PRODUCTS,
    DELL_U2723QE,
    DELL_XPS,
    FEATURED,
    HP_15,
    LENOVO_T14,
    LG_27UL500,
    LOGI_MX3S,
    NAME_A_TO_Z,
    PRICE_HIGH_TO_LOW,
    PRICE_LOW_TO_HIGH,
    givenTheCatalog,
} from './base/browse-catalog-background.js';

// Isolated: stages the shared ERP stub's product list, which every catalog page reads.
// Filtering, search and sort exist only in the UI; the API serves the unfiltered list.

const brandFilterCases = [
    { label: 'Dell', brands: ['Dell'], products: [DELL_XPS, DELL_U2723QE] },
    { label: 'Dell_LG', brands: ['Dell', 'LG'], products: [DELL_XPS, DELL_U2723QE, LG_27UL500] },
    { label: 'none', brands: [], products: ALL_PRODUCTS },
];

const searchCases = [
    { label: 'thinkpad', term: 'thinkpad', products: [LENOVO_T14] },
    { label: 'LOGITECH', term: 'LOGITECH', products: [LOGI_MX3S] },
    { label: 'wireless', term: 'wireless', products: [LOGI_MX3S] },
    { label: '4k', term: '4k', products: [DELL_U2723QE, LG_27UL500] },
    { label: 'empty', term: '', products: ALL_PRODUCTS },
];

const sortCases = [
    { label: 'featured', sort: FEATURED, products: ALL_PRODUCTS },
    { label: 'priceLowToHigh', sort: PRICE_LOW_TO_HIGH, products: [LOGI_MX3S, LG_27UL500, DELL_U2723QE, HP_15, LENOVO_T14, DELL_XPS] },
    { label: 'priceHighToLow', sort: PRICE_HIGH_TO_LOW, products: [DELL_XPS, LENOVO_T14, HP_15, DELL_U2723QE, LG_27UL500, LOGI_MX3S] },
    { label: 'nameAToZ', sort: NAME_A_TO_Z, products: [DELL_U2723QE, DELL_XPS, HP_15, LENOVO_T14, LG_27UL500, LOGI_MX3S] },
];

test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });

    forChannels(ChannelType.UI, ChannelType.API)(() => {
        test('shouldListEveryErpProductInTheErpsOrder', criteria('UC-006.1'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .browseCatalog()
                .then()
                .shouldSucceed()
                .catalog()
                .showsProducts(...ALL_PRODUCTS)
                .hasProductCount(6);
        });

        test('shouldLetAnAnonymousVisitorBrowseWithoutSigningIn', criteria('UC-006.8'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .and()
                .notLoggedIn()
                .when()
                .browseCatalog()
                .then()
                .shouldSucceed()
                .catalog()
                .hasProductCount(6);
        });

        test('shouldShowASignedInCustomerTheSameCatalog', criteria('UC-006.8'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .and()
                .loggedInAsCustomer()
                .when()
                .browseCatalog()
                .then()
                .shouldSucceed()
                .catalog()
                .showsProducts(...ALL_PRODUCTS);
        });
    });

    forChannels(ChannelType.UI)(() => {
        test('shouldShowTheTitleBrandAndPriceOfEachProduct', criteria('UC-006.1'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .browseCatalog()
                .then()
                .shouldSucceed()
                .catalog()
                .showsProduct(DELL_XPS, 'Dell XPS 13', 'Dell', '$1,299.99')
                .showsProduct(LOGI_MX3S, 'Logitech MX Master 3S', 'Logitech', '$99.99');
        });

        test('shouldOfferTheCategoriesOfTheCatalogsProducts', criteria('UC-006.2'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .browseCatalog()
                .then()
                .shouldSucceed()
                .catalog()
                .offersCategories('All', 'Laptops', 'Monitors', 'Accessories')
                .hasCategorySelected('All');
        });

        test('shouldShowOnlyTheProductsInTheChosenCategory', criteria('UC-006.2'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .browseCatalog()
                .withCategory('Monitors')
                .then()
                .shouldSucceed()
                .catalog()
                .showsProducts(DELL_U2723QE, LG_27UL500)
                .hasProductCount(2);
        });

        test('shouldFilterByCategoryFromTheHeaderLink', criteria('UC-006.2'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .browseCatalog()
                .withHeaderCategoryLink('Laptops')
                .then()
                .shouldSucceed()
                .catalog()
                .showsProducts(HP_15, DELL_XPS, LENOVO_T14)
                .hasCategorySelected('Laptops');
        });

        test('shouldOfferTheBrandsOfTheCatalogsProducts', criteria('UC-006.3'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .browseCatalog()
                .then()
                .shouldSucceed()
                .catalog()
                .offersBrands('HP', 'Dell', 'Lenovo', 'LG', 'Logitech')
                .hasNoBrandTicked();
        });

        test.each(brandFilterCases)(
            'shouldShowOnlyTheProductsOfTheTickedBrands_$label',
            criteria('UC-006.3'),
            async ({ scenario, brands, products }) => {
                await givenTheCatalog(scenario)
                    .when()
                    .browseCatalog()
                    .withBrands(...brands)
                    .then()
                    .shouldSucceed()
                    .catalog()
                    .showsProducts(...products);
            },
        );

        test.each(searchCases)(
            'shouldFindProductsByTitleBrandOrDescriptionIgnoringCase_$label',
            criteria('UC-006.4'),
            async ({ scenario, term, products }) => {
                await givenTheCatalog(scenario)
                    .when()
                    .browseCatalog()
                    .withSearch(term)
                    .then()
                    .shouldSucceed()
                    .catalog()
                    .showsProducts(...products);
            },
        );

        test('shouldShowOnlyProductsThatMatchEveryFilter', criteria('UC-006.5'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .browseCatalog()
                .withCategory('Laptops')
                .withBrands('Dell')
                .then()
                .shouldSucceed()
                .catalog()
                .showsProducts(DELL_XPS)
                .hasProductCount(1);
        });

        test.each(sortCases)('shouldSortTheProducts_$label', criteria('UC-006.6'), async ({ scenario, sort, products }) => {
            await givenTheCatalog(scenario)
                .when()
                .browseCatalog()
                .withSort(sort)
                .then()
                .shouldSucceed()
                .catalog()
                .showsProducts(...products);
        });

        test('shouldSortOnlyTheFilteredProducts', criteria('UC-006.6'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .browseCatalog()
                .withCategory('Laptops')
                .withSort(PRICE_HIGH_TO_LOW)
                .then()
                .shouldSucceed()
                .catalog()
                .showsProducts(DELL_XPS, LENOVO_T14, HP_15);
        });

        test('shouldShowTheSameFilteredViewWhenThePageAddressIsOpenedAgain', criteria('UC-006.7'), async ({ scenario }) => {
            await givenTheCatalog(scenario)
                .when()
                .browseCatalog()
                .withCategory('Laptops')
                .withSort(PRICE_HIGH_TO_LOW)
                .withPageAddressReopened()
                .then()
                .shouldSucceed()
                .catalog()
                .showsProducts(DELL_XPS, LENOVO_T14, HP_15)
                .hasCategorySelected('Laptops')
                .hasSort(PRICE_HIGH_TO_LOW);
        });
    });
});
