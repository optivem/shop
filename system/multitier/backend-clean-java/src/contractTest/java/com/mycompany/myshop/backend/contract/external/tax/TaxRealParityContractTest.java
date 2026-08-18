package com.mycompany.myshop.backend.contract.external.tax;

import com.mycompany.myshop.backend.contract.external.ExternalSystemSimulator;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.infrastructure.external.tax.HttpTaxGateway;
import com.mycompany.myshop.backend.testkit.driver.adapter.external.tax.client.SimulatorTaxCountryClient;

class TaxRealParityContractTest extends BaseTaxCountryParityContractTest {

    private static final String BASE_URL = ExternalSystemSimulator.baseUrl("/tax");

    private final SimulatorTaxCountryClient client = new SimulatorTaxCountryClient(BASE_URL);

    private final TaxGateway taxGateway = new HttpTaxGateway(BASE_URL);

    @Override
    protected void arrangeCountry(String code, String taxRate) {
        client.createCountry(code, taxRate);
    }

    @Override
    protected TaxGateway taxGateway() {
        return taxGateway;
    }
}
