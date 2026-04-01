package com.optivem.eshop.systemtest.legacy.mod10.acceptance;

import com.optivem.eshop.systemtest.legacy.mod10.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.testing.Channel;
import com.optivem.testing.DataSource;
import org.junit.jupiter.api.TestTemplate;

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
}



