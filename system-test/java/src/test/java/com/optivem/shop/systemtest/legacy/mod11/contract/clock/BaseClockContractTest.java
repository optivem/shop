package com.optivem.shop.systemtest.legacy.mod11.contract.clock;

import com.optivem.shop.systemtest.legacy.mod11.contract.base.BaseExternalSystemContractTest;
import org.junit.jupiter.api.Test;

public abstract class BaseClockContractTest extends BaseExternalSystemContractTest {
    @Test
    void shouldBeAbleToGetTime() {
        scenario
                .given()
                .then().clock().hasTime();
    }
}
