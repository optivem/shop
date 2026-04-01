package com.optivem.eshop.systemtest.legacy.mod11.contract.erp;

import com.optivem.eshop.systemtest.configuration.ExternalSystemMode;

public class ErpStubContractTest extends BaseErpContractTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }
}



