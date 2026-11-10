import type { ScenarioDsl } from '../../../../src/testkit/dsl/port/scenario-dsl.js';
import type { GivenProducts } from '../../../../src/testkit/dsl/port/given/steps/given-products.js';

// UC-006's Background: the ERP's product list every Browse Catalog scenario starts from.
export const HP_15 = 'HP-15';
export const DELL_XPS = 'DELL-XPS';
export const LENOVO_T14 = 'LENOVO-T14';
export const DELL_U2723QE = 'DELL-U2723QE';
export const LG_27UL500 = 'LG-27UL500';
export const LOGI_MX3S = 'LOGI-MX3S';
export const ALL_PRODUCTS = [HP_15, DELL_XPS, LENOVO_T14, DELL_U2723QE, LG_27UL500, LOGI_MX3S];

export const FEATURED = 'Featured';
export const PRICE_LOW_TO_HIGH = 'Price: low to high';
export const PRICE_HIGH_TO_LOW = 'Price: high to low';
export const NAME_A_TO_Z = 'Name: A to Z';

/** Given the ERP has these products, in this order. */
export function givenTheCatalog(scenario: ScenarioDsl): GivenProducts {
    return scenario
        .given()
        .products()
        .withProduct(HP_15, 'HP Laptop 15', 'HP', 'Laptops', '699.99', 'Everyday 15-inch laptop')
        .withProduct(DELL_XPS, 'Dell XPS 13', 'Dell', 'Laptops', '1299.99', 'Compact 13-inch ultrabook')
        .withProduct(LENOVO_T14, 'Lenovo ThinkPad T14', 'Lenovo', 'Laptops', '999.99', 'Business laptop')
        .withProduct(DELL_U2723QE, 'Dell UltraSharp 27 4K', 'Dell', 'Monitors', '579.99', '27-inch USB-C monitor')
        .withProduct(LG_27UL500, 'LG 27-inch 4K UHD Monitor', 'LG', 'Monitors', '299.99', 'HDR10 monitor')
        .withProduct(LOGI_MX3S, 'Logitech MX Master 3S', 'Logitech', 'Accessories', '99.99', 'Wireless mouse');
}
