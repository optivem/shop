package com.mycompany.myshop.backend.component.latest;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

class OrderHistoryComponentTest extends BaseComponentTest {

    @Test
    void browseReturnsPlacedOrders() {
        scenario.when().placeOrder()
            .then().shouldSucceed()
            .and().orderHistory().containsOrder();
    }

    @Test
    void viewMissingOrderReturnsNotFound() {
        scenario.when().viewOrder().withOrderNumber("UNKNOWN")
            .then().shouldFail()
                .errorMessage("Order UNKNOWN does not exist.");
    }
}
