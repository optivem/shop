package com.mycompany.myshop.backend.component.latest.contract;

import com.mycompany.myshop.backend.AbstractComponentTest;
import org.junit.jupiter.api.Test;

/**
 * Pins that the component layer's ERP WireMock stub is CONSUMABLE BY THE SUT. The stub's JSON is
 * hand-written in {@code ErpStubDriver} and shares no source with system-test's typed DTO, so
 * system-test's own {@code ErpStubContractTest} does not transitively prove this stub parses. The
 * read-back goes through the SUT's production {@code ErpGateway} (real HTTP + real
 * {@code ProductDetailsResponse} parse), so a field-name drift in the stub (e.g. {@code price}→
 * {@code cost}) fails this test instead of silently yielding null.
 */
class ErpStubContractComponentTest extends AbstractComponentTest {

    @Test
    void stubProductIsConsumableBySut() {
        scenario
            .given().product().withSku("BOOK-123").withUnitPrice(10.00)
            .then().product("BOOK-123").hasSku("BOOK-123").hasPrice(10.00);
    }
}
