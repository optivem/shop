package com.optivem.eshop.systemtest.legacy.mod11.contract.erp;

import com.optivem.eshop.systemtest.legacy.mod11.contract.base.BaseExternalSystemContractTest;
import org.junit.jupiter.api.Test;

public abstract class BaseErpContractTest extends BaseExternalSystemContractTest {
    @Test
    void shouldBeAbleToGetProduct() {
        scenario
                .given().product().withSku("SKU-123").withUnitPrice(12.0)
                .then().product("SKU-123").hasSku("SKU-123").hasPrice(12.0);
    }

}
