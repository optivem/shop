package com.mycompany.myshop.systemtest.latest.acceptance.base;

import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenProducts;

/** UC-006's Background: the ERP's product list every Browse Catalog scenario starts from. */
public abstract class BaseBrowseCatalogTest extends BaseAcceptanceTest {
    protected static final String HP_15 = "HP-15";
    protected static final String DELL_XPS = "DELL-XPS";
    protected static final String LENOVO_T14 = "LENOVO-T14";
    protected static final String DELL_U2723QE = "DELL-U2723QE";
    protected static final String LG_27UL500 = "LG-27UL500";
    protected static final String LOGI_MX3S = "LOGI-MX3S";
    protected static final String[] ALL_PRODUCTS = {HP_15, DELL_XPS, LENOVO_T14, DELL_U2723QE, LG_27UL500, LOGI_MX3S};

    protected static final String FEATURED = "Featured";
    protected static final String PRICE_LOW_TO_HIGH = "Price: low to high";
    protected static final String PRICE_HIGH_TO_LOW = "Price: high to low";
    protected static final String NAME_A_TO_Z = "Name: A to Z";

    /** Given the ERP has these products, in this order. */
    protected GivenProducts givenTheCatalog() {
        return scenario.given().products()
                .withProduct(HP_15, "HP Laptop 15", "HP", "Laptops", "699.99", "Everyday 15-inch laptop")
                .withProduct(DELL_XPS, "Dell XPS 13", "Dell", "Laptops", "1299.99", "Compact 13-inch ultrabook")
                .withProduct(LENOVO_T14, "Lenovo ThinkPad T14", "Lenovo", "Laptops", "999.99", "Business laptop")
                .withProduct(DELL_U2723QE, "Dell UltraSharp 27 4K", "Dell", "Monitors", "579.99", "27-inch USB-C monitor")
                .withProduct(LG_27UL500, "LG 27-inch 4K UHD Monitor", "LG", "Monitors", "299.99", "HDR10 monitor")
                .withProduct(LOGI_MX3S, "Logitech MX Master 3S", "Logitech", "Accessories", "99.99", "Wireless mouse");
    }
}
