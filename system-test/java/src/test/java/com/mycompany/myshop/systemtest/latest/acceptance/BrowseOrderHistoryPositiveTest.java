package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_ORDER_NUMBER;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_SKU;

class BrowseOrderHistoryPositiveTest extends BaseAcceptanceTest {
    @Tag("UC-002.1")
    @Tag("UC-002.2")
    @Tag("UC-011.5")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void customerShouldSeeOwnOrderInHistory() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsCustomer()
                .when().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder(DEFAULT_ORDER_NUMBER);
    }

    @Tag("UC-011.1")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldSeeCustomerOrderInHistory() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsAdmin()
                .when().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder(DEFAULT_ORDER_NUMBER);
    }

    @Tag("UC-011.1")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldSeeAnotherCustomersOrderInHistory() {
        scenario
                .given().order()
                    .placedByCustomer("B")
                .and().loggedInAsAdmin()
                .when().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder(DEFAULT_ORDER_NUMBER);
    }

    @Tag("UC-002.3")
    @TestTemplate
    @Channel(ChannelType.API)
    void firstPageShouldReportTheTotalNumberOfMatchingOrders() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsCustomer()
                .when().browseOrderHistory()
                    .withPage("1")
                    .withSize("1")
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder(DEFAULT_ORDER_NUMBER)
                        .hasTotalElements(1);
    }

    @Tag("UC-002.3")
    @TestTemplate
    @Channel(ChannelType.API)
    void pagePastTheLastShouldBeEmpty() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsCustomer()
                .when().browseOrderHistory()
                    .withPage("2")
                .then().shouldSucceed()
                    .orderHistory()
                        .isEmpty();
    }

    @Tag("UC-011.3")
    @Tag("UC-011.4")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldSeeOrderOfChosenCustomer() {
        scenario
                .given().order()
                    .withOrderNumber("ORD-A")
                    .placedByCustomer("A")
                .and().loggedInAsAdmin()
                .when().browseOrderHistory()
                    .withCustomer("A")
                    .withOrderNumber("ORD-A")
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder("ORD-A");
    }

    @Tag("UC-011.5")
    @TestTemplate
    @Channel(ChannelType.API)
    void customerFilterShouldBeIgnoredForCustomer() {
        scenario
                .given().order()
                    .withOrderNumber("ORD-A")
                    .placedByCustomer("A")
                .and().order()
                    .withOrderNumber("ORD-B")
                    .placedByCustomer("B")
                .and().loggedInAsCustomer("A")
                .when().browseOrderHistory()
                    .withCustomer("B")
                    .withOrderNumber("ORD-A")
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder("ORD-A");
    }

    // Not covered: the "newest first" order of @UC-011.1 (the given orders cannot be dated) and @UC-011.2 (an order
    // with no recorded customer cannot be placed).
    @Tag("UC-011.1")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldShowTheDetailsOfEachOrderInTheList() {
        scenario
                .given().order()
                    .withOrderNumber("ORD-B")
                    .withQuantity(2)
                    .placedByCustomer("B")
                .and().loggedInAsAdmin()
                .when().browseOrderHistory()
                    .withOrderNumber("ORD-B")
                .then().shouldSucceed()
                    .orderHistory()
                        .showsOrder("ORD-B", "B", DEFAULT_SKU, 2, OrderStatus.PLACED);
    }

    // Opening the linked order is ViewOrderPositiveTest's adminShouldBeAbleToViewAnotherCustomersOrder.
    @Tag("UC-011.1")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldLinkEachOrderToItsDetails() {
        scenario
                .given().order()
                    .withOrderNumber("ORD-B")
                    .placedByCustomer("B")
                .and().loggedInAsAdmin()
                .when().browseOrderHistory()
                    .withOrderNumber("ORD-B")
                .then().shouldSucceed()
                    .orderHistory()
                        .linksOrderToItsDetails("ORD-B");
    }

    @Tag("UC-011.3")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldOfferTheCustomersWhoHavePlacedOrders() {
        scenario
                .given().order()
                    .withOrderNumber("ORD-A")
                    .placedByCustomer("A")
                .and().order()
                    .withOrderNumber("ORD-B")
                    .placedByCustomer("B")
                .and().loggedInAsAdmin()
                .when().browseOrderHistory()
                    .withNoOrderNumberFilter()
                .then().shouldSucceed()
                    .orderHistory()
                        .offersCustomers("A", "B")
                        .hasCustomerFilterSetToAllCustomers();
    }

    @Tag("UC-011.3")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldShowOnlyTheOrdersOfTheChosenCustomer() {
        // The chosen customer is a new one: a shared customer has more orders than fit on page 1, all at the same stub
        // clock time, so its new order would not reliably be on the first page.
        scenario
                .given().newCustomer()
                .and().order()
                    .withOrderNumber("ORD-A")
                    .placedByCustomer()
                .and().order()
                    .withOrderNumber("ORD-B")
                    .placedByCustomer("B")
                .and().loggedInAsAdmin()
                .when().browseOrderHistory()
                    .withCustomer()
                    .withNoOrderNumberFilter()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder("ORD-A")
                        .doesNotContainOrder("ORD-B");
    }

    @Tag("UC-011.7")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldNotShowACustomerTheCustomerColumnOrTheCustomerFilter() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsCustomer()
                .when().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder(DEFAULT_ORDER_NUMBER)
                        .showsNoCustomers()
                        .offersNoCustomerFilter();
    }

    @Tag("UC-002.3")
    @TestTemplate
    @Channel(ChannelType.API)
    void shouldShowTheFirstPageOf50OrdersByDefault() {
        scenario
                .given().loggedInAsCustomer()
                .when().browseOrderHistory()
                    .withNoOrderNumberFilter()
                .then().shouldSucceed()
                    .orderHistory()
                        .hasPage(1)
                        .hasPageSize(50);
    }

    // Opening the linked order is ViewOrderPositiveTest's customerShouldBeAbleToViewOwnOrder.
    @Tag("UC-002.1")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldLinkEachOfTheCustomersOrdersToItsDetails() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsCustomer()
                .when().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .linksOrderToItsDetails(DEFAULT_ORDER_NUMBER);
    }
}
