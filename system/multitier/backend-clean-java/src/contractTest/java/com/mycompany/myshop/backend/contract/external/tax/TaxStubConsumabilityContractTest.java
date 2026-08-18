package com.mycompany.myshop.backend.contract.external.tax;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

class TaxStubConsumabilityContractTest extends BaseComponentTest {

    @Test
    void stubTaxIsConsumableBySut() {
        scenario
            .given().country().withCode("US").withTaxRate(0.09)
            .then().country("US").hasTaxRate(0.09);
    }
}
