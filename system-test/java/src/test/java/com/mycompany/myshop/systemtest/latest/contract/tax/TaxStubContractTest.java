package com.mycompany.myshop.systemtest.latest.contract.tax;

import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TaxStubContractTest extends BaseTaxContractTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }

    @Test
    void shouldBeAbleToGetConfiguredTaxRate() {
        assertDoesNotThrow(() -> scenario
                .given().country().withCode("LALA").withTaxRate(0.23)
                .then().country("LALA").hasCountry("LALA").hasTaxRate(0.23));
    }
}
