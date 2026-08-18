package com.mycompany.myshop.backend.contract.external.erp;

import com.mycompany.myshop.backend.contract.external.ExternalSystemSimulator;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.infrastructure.external.erp.HttpErpGateway;
import com.mycompany.myshop.backend.testkit.driver.adapter.external.erp.client.SimulatorErpProductClient;

class ErpRealParityContractTest extends BaseErpProductParityContractTest {

    private static final String BASE_URL = ExternalSystemSimulator.baseUrl("/erp");

    private final SimulatorErpProductClient client = new SimulatorErpProductClient(BASE_URL);

    private final ErpGateway erpGateway = new HttpErpGateway(BASE_URL);

    @Override
    protected void arrangeProduct(String sku, String price) {
        client.createProduct(sku, price);
    }

    @Override
    protected ErpGateway erpGateway() {
        return erpGateway;
    }
}
