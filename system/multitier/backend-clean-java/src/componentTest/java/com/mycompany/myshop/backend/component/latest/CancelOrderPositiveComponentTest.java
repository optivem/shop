package com.mycompany.myshop.backend.component.latest;

import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CancelOrderPositiveComponentTest extends BaseComponentTest {

    @Test
    void cancelledOrderHasCancelledStatus() {
        scenario.given().order()
            .when().cancelOrder()
            .then().shouldSucceed()
            .and().order().hasStatus(OrderStatus.CANCELLED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2024-12-31T21:59:59Z",
        "2024-12-31T22:30:01Z",
        "2024-12-31T10:00:00Z",
        "2025-01-01T22:15:00Z"
    })
    void cancelSucceedsOutsideTheYearEndBlackout(String time) {
        scenario.given().clock().withTime(time)
            .and().order()
            .when().cancelOrder()
            .then().shouldSucceed()
            .and().order().hasStatus(OrderStatus.CANCELLED);
    }
}
