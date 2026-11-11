package com.mycompany.myshop.testkit.dsl.core.scenario.given;

/** The ERP product lists the scenario staged, so a When step can change them after the cart was filled. */
public interface StagedProducts {
    StagedProducts NONE = (skuAlias, unitPrice) -> {
        throw new IllegalStateException("No ERP product list was staged, so no price can change");
    };

    /** Stages the product lists again, with this product at the new price. */
    void changePrice(String skuAlias, String unitPrice);
}
