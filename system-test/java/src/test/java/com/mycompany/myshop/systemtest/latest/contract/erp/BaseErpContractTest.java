package com.mycompany.myshop.systemtest.latest.contract.erp;

import com.mycompany.myshop.systemtest.latest.contract.base.BaseExternalSystemContractTest;
import org.junit.jupiter.api.Test;

public abstract class BaseErpContractTest extends BaseExternalSystemContractTest {
    @Test
    void shouldBeAbleToGetProduct() {
        scenario
                .given().product().withSku("SKU-123").withUnitPrice(12.0)
                .then().product("SKU-123").hasSku("SKU-123").hasPrice(12.0);
    }

    @Test
    void shouldBeAbleToGetProductWeight() {
        scenario
                .given().product().withSku("SKU-456").withWeight(2.5)
                .then().product("SKU-456").hasWeight(2.5);
    }
}
