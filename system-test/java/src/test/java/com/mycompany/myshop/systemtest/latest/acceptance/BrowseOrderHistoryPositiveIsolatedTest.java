package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;
import com.mycompany.myshop.testkit.dsl.port.given.GivenStage;
import com.optivem.testing.Channel;
import com.optivem.testing.DataSource;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * My orders for a customer of their own, whose orders are placed at set times. The orders are placed out of date order
 * (ORD-1002, ORD-1001, ORD-1003), so a list in placing order would not pass for one in date order. Also the admin's
 * unfiltered list of all orders, whose orders are placed after every other order so they are on its first page.
 */
@Isolated("sets the shared clock to place each order at its own time; parallel runs would clash on the clock")
class BrowseOrderHistoryPositiveIsolatedTest extends BaseAcceptanceTest {
    private static final String NONE = "(none)";

    private GivenStage givenANewCustomersThreeOrders() {
        return scenario
                .given().newCustomer()
                .and().order()
                    .withOrderNumber("ORD-1002")
                    .placedAt("2026-01-12T14:30:00Z")
                .and().order()
                    .withOrderNumber("ORD-1001")
                    .placedAt("2026-01-10T09:00:00Z")
                .and().order()
                    .withOrderNumber("ORD-1003")
                    .placedAt("2026-01-15T08:00:00Z")
                .and().loggedInAsCustomer();
    }

    @Tag("UC-002.1")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldListTheCustomersOrdersNewestFirst() {
        givenANewCustomersThreeOrders()
                .when().browseOrderHistory()
                    .withNoOrderNumberFilter()
                .then().shouldSucceed()
                    .orderHistory()
                        .showsOrders("ORD-1003", "ORD-1002", "ORD-1001");
    }

    @Tag("UC-002.1")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldShowTheSummaryOfEachOrder() {
        scenario
                .given().product()
                    .withSku("BOOK-123")
                    .withUnitPrice("20.00")
                .and().country()
                    .withCode("US")
                    .withTaxRate("0.10")
                .and().coupon()
                    .withCouponCode("SUMMER2025")
                    .withDiscountRate("0.10")
                .and().order()
                    .withOrderNumber("ORD-1001")
                    .withSku("BOOK-123")
                    .withQuantity(5)
                    .withCountry("US")
                    .withCouponCode("SUMMER2025")
                    .placedAt("2026-01-10T09:00:00Z")
                .and().loggedInAsCustomer()
                .when().browseOrderHistory()
                    .withOrderNumber("ORD-1001")
                .then().shouldSucceed()
                    .orderHistory()
                        .showsOrderSummary("ORD-1001", "2026-01-10T09:00:00Z", "BOOK-123", 5, "99.00",
                                OrderStatus.PLACED);
    }

    @Tag("UC-002.2")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldFilterOrdersByPartOfTheOrderNumberIgnoringCase() {
        givenANewCustomersThreeOrders()
                .when().browseOrderHistory()
                    .withOrderNumberPartOf("ORD-1002")
                .then().shouldSucceed()
                    .orderHistory()
                        .showsOrders("ORD-1002");
    }

    // The spec's "ord-100" is the part every order number here shares: the generated numbers share only "ORD-".
    @Tag("UC-002.2")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldShowEveryOrderWhoseNumberHasTheFilterIgnoringCase() {
        givenANewCustomersThreeOrders()
                .when().browseOrderHistory()
                    .withOrderNumber("ord-")
                .then().shouldSucceed()
                    .orderHistory()
                        .showsOrders("ORD-1003", "ORD-1002", "ORD-1001");
    }

    @Tag("UC-002.2")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldShowEveryOrderWhenTheFilterIsEmpty() {
        givenANewCustomersThreeOrders()
                .when().browseOrderHistory()
                    .withOrderNumber("")
                .then().shouldSucceed()
                    .orderHistory()
                        .showsOrders("ORD-1003", "ORD-1002", "ORD-1001");
    }

    @Tag("UC-002.3")
    @TestTemplate
    @Channel(ChannelType.API)
    @DataSource({"1", "ORD-1003,ORD-1002"})
    @DataSource({"2", "ORD-1001"})
    @DataSource({"3", NONE})
    void shouldReadTheOrdersOnePageAtATime(String page, String orders) {
        givenANewCustomersThreeOrders()
                .when().browseOrderHistory()
                    .withNoOrderNumberFilter()
                    .withPage(page)
                    .withSize("2")
                .then().shouldSucceed()
                    .orderHistory()
                        .showsOrders(NONE.equals(orders) ? new String[0] : orders.split(","))
                        .hasTotalElements(3);
    }

    @Tag("UC-011.8")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowEveryOrderAgainAfterResettingTheFilters() {
        // All orders, newest first, hold more than one page, so ORD-A and ORD-B are placed after every other order
        // (a century from now, later on each run) to be on the first page once the filters are reset.
        var placedAt = Instant.now().plus(Duration.ofDays(36_500)).truncatedTo(ChronoUnit.SECONDS);
        scenario
                .given().order()
                    .withOrderNumber("ORD-A")
                    .placedByCustomer("A")
                    .placedAt(placedAt.toString())
                .and().order()
                    .withOrderNumber("ORD-B")
                    .placedByCustomer("B")
                    .placedAt(placedAt.plusSeconds(60).toString())
                .and().loggedInAsAdmin()
                .when().browseOrderHistory()
                    .withCustomer("B")
                    .withOrderNumber("ORD-A")
                    .withFiltersResetAfterwards()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder("ORD-A")
                        .containsOrder("ORD-B")
                        .hasCustomerFilterSetToAllCustomers();
    }
}
