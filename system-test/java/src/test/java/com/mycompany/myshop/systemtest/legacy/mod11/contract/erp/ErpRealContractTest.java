package com.mycompany.myshop.systemtest.legacy.mod11.contract.erp;

import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;

class ErpRealContractTest extends BaseErpContractTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}



