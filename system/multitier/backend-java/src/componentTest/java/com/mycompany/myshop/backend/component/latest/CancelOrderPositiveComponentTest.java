package com.mycompany.myshop.backend.component.latest;

import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.core.entities.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The fast twin of the {@code CancelOrderPositive*Test} system tests.
 *
 * <p>The blackout cases are the reason this class earns its place. In system-test they need {@code
 * @Isolated}, because moving the wall clock to a year-end time mutates state the whole suite shares.
 * Here the clock is a per-test WireMock stub, so the same scenarios run in parallel with everything
 * else and need no stack at all.
 */
class CancelOrderPositiveComponentTest extends BaseComponentTest {

    @Test
    void cancelledOrderHasCancelledStatus() {
        scenario.given().order()
            .when().cancelOrder()
            .then().shouldSucceed()
            .and().order().hasStatus(OrderStatus.CANCELLED);
    }

    /**
     * The blackout is December 31st 22:00–22:30 inclusive. These are the times just outside it — a
     * second before, a second after, another hour of the same day, and the same time on a different
     * day — each of which must still cancel.
     */
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
