package com.optivem.eshop.systemtest.legacy.mod05.e2e;

import com.optivem.eshop.systemtest.legacy.mod05.e2e.base.BaseE2eTest;
import org.junit.jupiter.api.Test;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;
import static org.assertj.core.api.Assertions.assertThat;

abstract class ViewOrderNegativeBaseTest extends BaseE2eTest {
    @Test
    void shouldNotBeAbleToViewNonExistentOrder() {
        var orderNumber = "NON-EXISTENT-ORDER-99999";

        var result = shopDriver.viewOrder(orderNumber);

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("Order NON-EXISTENT-ORDER-99999 does not exist.");
    }
}

