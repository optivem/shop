package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.driver.port.dtos.OrderStatus;
import com.optivem.testing.Channel;
import com.optivem.testing.DataSource;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class CancelOrderNegativeTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.API})
    @DataSource({"NON-EXISTENT-ORDER-99999", "Order NON-EXISTENT-ORDER-99999 does not exist."})
    @DataSource({"NON-EXISTENT-ORDER-88888", "Order NON-EXISTENT-ORDER-88888 does not exist."})
    @DataSource({"NON-EXISTENT-ORDER-77777", "Order NON-EXISTENT-ORDER-77777 does not exist."})
    void shouldNotCancelNonExistentOrder(String orderNumber, String expectedErrorMessage) {
        scenario
                .when().cancelOrder()
                    .withOrderNumber(orderNumber)
                .then().shouldFail()
                    .errorMessage(expectedErrorMessage);
    }

    @TestTemplate
    @Channel({ChannelType.API})
    void shouldNotCancelAlreadyCancelledOrder() {
        scenario
                .given().order()
                    .withStatus(OrderStatus.CANCELLED)
                .when().cancelOrder()
                .then().shouldFail()
                    .errorMessage("Order has already been cancelled");
    }

    @TestTemplate
    @Channel({ChannelType.API})
    void cannotCancelNonExistentOrder() {
        scenario
                .when().cancelOrder()
                    .withOrderNumber("non-existent-order-12345")
                .then().shouldFail()
                    .errorMessage("Order non-existent-order-12345 does not exist.");
    }

    @EnabledIfEnvironmentVariable(named = "GH_OPTIVEM_RUN_WIP_TESTS", matches = "1", disabledReason = "Work-in-progress test; set GH_OPTIVEM_RUN_WIP_TESTS=1 to run")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void cannotCancelAnOrderAt2245OnDecember31st() {
        scenario
                .given().product()
                    .withSku("DELL-XPS")
                    .withUnitPrice(1299.99)
                .and().order()
                    .withSku("DELL-XPS")
                    .withQuantity(1)
                    .withStatus(OrderStatus.PLACED)
                .and().clock()
                    .withTime("2025-12-31T22:45:00Z")
                .when().cancelOrder()
                .then().shouldFail()
                    .errorMessage("Order cancellation is not allowed on December 31st between 22:00 and 23:00")
                .and().order()
                    .hasStatus(OrderStatus.PLACED);
    }
}
