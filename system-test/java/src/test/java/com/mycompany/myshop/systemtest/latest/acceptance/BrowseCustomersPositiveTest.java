package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

// API only: the UI offers the customers as the order history's customer filter, which
// BrowseOrderHistoryPositiveTest.shouldOfferTheCustomersWhoHavePlacedOrders covers.
class BrowseCustomersPositiveTest extends BaseAcceptanceTest {
    // Shared stack: every customer who has ever ordered is listed, so only A and B's relative order is asserted.
    @Tag("UC-011.3")
    @TestTemplate
    @Channel(ChannelType.API)
    void shouldListEachCustomerWhoHasPlacedOrdersOnceFromAToZ() {
        scenario
                .given().order()
                    .withOrderNumber("ORD-A1")
                    .placedByCustomer("A")
                .and().order()
                    .withOrderNumber("ORD-B")
                    .placedByCustomer("B")
                .and().order()
                    .withOrderNumber("ORD-A2")
                    .placedByCustomer("A")
                .and().loggedInAsAdmin()
                .when().browseCustomers()
                .then().shouldSucceed()
                    .customerList()
                        .listsCustomersInOrder("A", "B");
    }
}
