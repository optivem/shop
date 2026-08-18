package com.mycompany.myshop.systemtest.legacy.mod11.contract.erp;

import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;

class ErpStubContractTest extends BaseErpContractTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }
}
