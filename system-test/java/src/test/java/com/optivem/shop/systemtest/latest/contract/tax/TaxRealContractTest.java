package com.optivem.shop.systemtest.latest.contract.tax;

import com.optivem.shop.systemtest.configuration.ExternalSystemMode;

public class TaxRealContractTest extends BaseTaxContractTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}
