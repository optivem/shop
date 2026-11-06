package com.mycompany.myshop.testkit.dsl.port.then.steps;

public interface ThenProducts {
    ThenProducts containsProduct(String skuAlias, String price);

    ThenProducts and();
}
