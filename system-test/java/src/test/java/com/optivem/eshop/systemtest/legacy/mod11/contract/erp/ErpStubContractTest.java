package com.optivem.eshop.systemtest.legacy.mod11.contract.erp;

import com.optivem.eshop.systemtest.configuration.ExternalSystemMode;

class ErpStubContractTest extends BaseErpContractTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }
}



