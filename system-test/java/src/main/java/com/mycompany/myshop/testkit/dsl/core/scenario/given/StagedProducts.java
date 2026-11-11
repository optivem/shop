package com.mycompany.myshop.testkit.dsl.core.scenario.given;

/** The ERP product lists the scenario staged, so a When step can change them after the cart was filled. */
public interface StagedProducts {
    StagedProducts NONE = new StagedProducts() {
        @Override
        public void changePrice(String skuAlias, String unitPrice) {
            throw new IllegalStateException("No ERP product list was staged, so no price can change");
        }

        @Override
        public void withdraw(String skuAlias) {
            throw new IllegalStateException("No ERP product list was staged, so no product can be withdrawn");
        }
    };

    /** Stages the product lists again, with this product at the new price. */
    void changePrice(String skuAlias, String unitPrice);

    /** The ERP no longer offers this product: the lists are staged again without it, and asking for it finds nothing. */
    void withdraw(String skuAlias);
}
