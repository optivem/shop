package com.mycompany.myshop.testkit.dsl.port.then;

import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenClock;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenCountry;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenProduct;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenProducts;

public interface ThenStage {
    ThenClock clock();

    ThenProduct product(String skuAlias);

    ThenProducts products();

    ThenCountry country(String countryAlias);
}
