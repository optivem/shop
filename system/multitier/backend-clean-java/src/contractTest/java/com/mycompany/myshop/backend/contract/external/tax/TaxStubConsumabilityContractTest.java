package com.mycompany.myshop.backend.contract.external.tax;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

/**
 * Pins that the component harness's Tax WireMock stub is CONSUMABLE BY THE SUT. The read-back goes
 * through the SUT's production {@code HttpTaxGateway} (real HTTP + real {@code TaxDetailsResponse}
 * parse), so a field drift in {@code TaxStubDriver} fails this test rather than silently yielding an
 * empty tax rate. See {@code ErpStubConsumabilityContractTest} for the full rationale.
 */
class TaxStubConsumabilityContractTest extends BaseComponentTest {

    @Test
    void stubTaxIsConsumableBySut() {
        scenario
            .given().country().withCode("US").withTaxRate(0.09)
            .then().country("US").hasTaxRate(0.09);
    }
}
