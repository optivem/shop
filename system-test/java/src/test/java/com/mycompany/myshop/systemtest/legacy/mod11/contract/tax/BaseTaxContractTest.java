package com.mycompany.myshop.systemtest.legacy.mod11.contract.tax;

import com.mycompany.myshop.systemtest.legacy.mod11.contract.base.BaseExternalSystemContractTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public abstract class BaseTaxContractTest extends BaseExternalSystemContractTest {
    @Test
    void shouldBeAbleToGetTaxRate() {
        assertDoesNotThrow(() -> scenario
                .given().country().withCode("US").withTaxRate("0.09")
                .then().country("US").hasTaxRateIsPositive());
    }
}
