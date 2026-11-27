package com.mycompany.myshop.testkit.dsl.port.given.steps;

import com.mycompany.myshop.testkit.dsl.port.given.steps.base.GivenStep;

/**
 * The ERP's enumerable product list. Unlike {@link GivenProduct} (a single product looked up by SKU),
 * this stages the whole list the ERP returns, in the order the products are added; with no products added,
 * the list is empty.
 */
public interface GivenProducts extends GivenStep {
    GivenProducts withProduct(String sku, String unitPrice);

    GivenProducts withProduct(String sku, String title, String brand, String category, String unitPrice, String description);

    /** The photo the ERP gives a product already added, as the ERP's own path (such as /erp/images/HP-15.jpg). */
    GivenProducts withImageUrl(String sku, String imageUrl);
}
