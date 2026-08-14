package com.mycompany.myshop.backend.contract.external.tax;

import com.mycompany.myshop.backend.contract.external.ExternalSystemSimulator;
import com.mycompany.myshop.backend.core.services.external.TaxGateway;
import com.mycompany.myshop.backend.testkit.driver.adapter.external.tax.client.SimulatorTaxCountryClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The {@code Real} side of the tax country contract: same two scenarios as
 * {@link TaxStubParityContractTest}, run against the actual tax simulator instead of a WireMock
 * stand-in. Closes the gap where {@code TaxStubDriver} was consumability-checked (its JSON parses
 * through the production gateway) but never held against the real system, so a field rename on the
 * tax side would have gone unnoticed until a deployed-system test caught it.
 *
 * <p>The simulator comes from {@link ExternalSystemSimulator} — see there for how it is started and
 * how to point this at an already-running instance instead.
 */
class TaxRealParityContractTest extends BaseTaxCountryParityContractTest {

    private static final String BASE_URL = ExternalSystemSimulator.baseUrl("/tax");

    private final SimulatorTaxCountryClient client = new SimulatorTaxCountryClient(BASE_URL);

    private TaxGateway taxGateway;

    @BeforeEach
    void setUp() {
        taxGateway = new TaxGateway();
        ReflectionTestUtils.setField(taxGateway, "taxUrl", BASE_URL);
    }

    @Override
    protected void arrangeCountry(String code, String taxRate) {
        client.createCountry(code, taxRate);
    }

    @Override
    protected TaxGateway taxGateway() {
        return taxGateway;
    }
}
