package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;
import com.optivem.testing.Channel;
import com.optivem.testing.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

import static com.mycompany.myshop.systemtest.commons.constants.ErrorMessages.PERMISSION_DENIED;

class DeliverOrderNegativeTest extends BaseAcceptanceTest {
    @Tag("UC-012.4")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void customerShouldNotBeAbleToDeliverOrder() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsCustomer()
                .when().deliverOrder()
                .then().shouldFail()
                    .errorMessage(PERMISSION_DENIED)
                .and().order()
                    .hasStatus(OrderStatus.PLACED);
    }

    @Tag("UC-012.5")
    @TestTemplate
    @Channel({ChannelType.API})
    void shouldNotDeliverNonExistentOrder() {
        scenario
                .given().loggedInAsAdmin()
                .when().deliverOrder()
                    .withOrderNumber("NON-EXISTENT-ORDER-99999")
                .then().shouldFail()
                    .errorMessage("Order NON-EXISTENT-ORDER-99999 does not exist.");
    }

    @Tag("UC-012.6")
    @TestTemplate
    @Channel({ChannelType.API})
    @DataSource({"CANCELLED"})
    @DataSource({"DELIVERED"})
    void shouldNotDeliverOrderThatIsNotPlaced(String status) {
        scenario
                .given().order()
                    .placedByCustomer()
                    .withStatus(status)
                .and().loggedInAsAdmin()
                .when().deliverOrder()
                .then().shouldFail()
                    .errorMessage("Order cannot be delivered in its current status")
                .and().order()
                    .hasStatus(OrderStatus.valueOf(status));
    }

    @Tag("UC-012.2")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldOfferNeitherDeliverNorCancelOnceTheOrderIsDelivered() {
        scenario
                .given().order()
                    .placedByCustomer()
                    .withStatus(OrderStatus.DELIVERED)
                .and().loggedInAsAdmin()
                .when().viewOrder()
                .then().shouldSucceed()
                .and().order()
                    .doesNotOfferDeliverOrder()
                    .doesNotOfferCancelOrder();
    }
}
