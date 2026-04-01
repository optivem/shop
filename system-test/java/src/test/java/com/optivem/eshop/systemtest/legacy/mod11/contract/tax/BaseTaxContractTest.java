package com.optivem.eshop.systemtest.legacy.mod11.contract.tax;

import com.optivem.eshop.systemtest.legacy.mod11.contract.base.BaseExternalSystemContractTest;
import org.junit.jupiter.api.Test;

public abstract class BaseTaxContractTest extends BaseExternalSystemContractTest {
    @Test
    void shouldBeAbleToGetTaxRate() {
        scenario
                .given().country().withCode("US").withTaxRate(0.09)
                .then().country("US").hasTaxRateIsPositive();
    }
}
