package com.mycompany.myshop.backend.contract.external.erp;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

class ErpStubConsumabilityContractTest extends BaseComponentTest {

    @Test
    void stubProductIsConsumableBySut() {
        scenario
            .given().product().withSku("BOOK-123").withUnitPrice(10.00)
            .then().product("BOOK-123").hasSku("BOOK-123").hasPrice(10.00);
    }
}
