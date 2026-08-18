package com.mycompany.myshop.backend.component.latest;

import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CancelOrderNegativeComponentTest extends BaseComponentTest {

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "ORD-DOES-NOT-EXIST"})
    void cancellingAnUnknownOrderIsRejected(String orderNumber) {
        scenario.when().cancelOrder().withOrderNumber(orderNumber)
            .then().shouldFail()
                .errorMessage("Order " + orderNumber + " does not exist.");
    }

    @Test
    void cancellingAnAlreadyCancelledOrderIsRejected() {
        scenario.given().order().withStatus(OrderStatus.CANCELLED)
            .when().cancelOrder()
            .then().shouldFail()
                .errorMessage("Order has already been cancelled");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2024-12-31T22:00:00Z",
        "2024-12-31T22:15:00Z",
        "2024-12-31T22:30:00Z"
    })
    void cancelIsRejectedInsideTheYearEndBlackout(String time) {
        scenario.given().clock().withTime(time)
            .and().order()
            .when().cancelOrder()
            .then().shouldFail()
                .errorMessage(
                    "Order cancellation is not allowed on December 31st between 22:00 and 23:00");
    }
}
