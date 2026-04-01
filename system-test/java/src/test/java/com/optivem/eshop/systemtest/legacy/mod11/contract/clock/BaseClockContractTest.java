package com.optivem.eshop.systemtest.legacy.mod11.contract.clock;

import com.optivem.eshop.systemtest.legacy.mod11.contract.base.BaseExternalSystemContractTest;
import org.junit.jupiter.api.Test;

public abstract class BaseClockContractTest extends BaseExternalSystemContractTest {
    @Test
    void shouldBeAbleToGetTime() {
        scenario
                .given().clock().withTime()
                .then().clock().hasTime();
    }
}
