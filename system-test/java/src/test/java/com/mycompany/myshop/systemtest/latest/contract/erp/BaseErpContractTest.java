package com.mycompany.myshop.systemtest.latest.contract.erp;

import com.mycompany.myshop.systemtest.latest.contract.base.BaseExternalSystemContractTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public abstract class BaseErpContractTest extends BaseExternalSystemContractTest {
    @Test
    void shouldBeAbleToGetProduct() {
        assertDoesNotThrow(() -> scenario
                .given().product().withSku("BOOK-123").withUnitPrice("12.00")
                .then().product("BOOK-123").hasSku("BOOK-123").hasPrice("12.00"));
    }

    @Test
    void shouldBeAbleToListProducts() {
        assertDoesNotThrow(() -> scenario
                .given().products()
                    .withProduct("BOOK-123", "12.00")
                    .withProduct("BOOK-456", "15.50")
                .then().products()
                    .containsProduct("BOOK-123", "12.00")
                    .containsProduct("BOOK-456", "15.50"));
    }
}
