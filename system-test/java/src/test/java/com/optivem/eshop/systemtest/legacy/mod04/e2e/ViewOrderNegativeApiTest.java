package com.optivem.eshop.systemtest.legacy.mod04.e2e;

import com.optivem.eshop.systemtest.legacy.mod04.e2e.base.BaseE2eTest;
import org.junit.jupiter.api.Test;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;
import static org.assertj.core.api.Assertions.assertThat;

class ViewOrderNegativeApiTest extends BaseE2eTest {
    @Override
    protected void setShopDriver() {
        setUpShopApiClient();
    }

    @Test
    void shouldNotBeAbleToViewNonExistentOrder() {
        var orderNumber = "NON-EXISTENT-ORDER-99999";

        var result = shopApiClient.orders().viewOrder(orderNumber);

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getDetail()).isEqualTo("Order NON-EXISTENT-ORDER-99999 does not exist.");
    }
}

