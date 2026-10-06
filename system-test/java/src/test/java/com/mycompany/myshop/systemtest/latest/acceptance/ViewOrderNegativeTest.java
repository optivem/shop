package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.systemtest.latest.acceptance.base.RequiresKeycloak;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_ORDER_NUMBER;
import static com.mycompany.myshop.systemtest.commons.constants.ErrorMessages.AUTHENTICATION_REQUIRED;

class ViewOrderNegativeTest extends BaseAcceptanceTest {
    private static Stream<Arguments> provideNonExistentOrderValues() {
        return Stream.of(
                Arguments.of("NON-EXISTENT-ORDER-99999", "Order NON-EXISTENT-ORDER-99999 does not exist."),
                Arguments.of("NON-EXISTENT-ORDER-88888", "Order NON-EXISTENT-ORDER-88888 does not exist."),
                Arguments.of("NON-EXISTENT-ORDER-77777", "Order NON-EXISTENT-ORDER-77777 does not exist.")
        );
    }

    @TestTemplate
    @Channel(value = {ChannelType.API}, alsoForFirstRow = ChannelType.UI)
    @MethodSource("provideNonExistentOrderValues")
    void shouldNotBeAbleToViewNonExistentOrder(String orderNumber, String expectedErrorMessage) {
        scenario
                .when().viewOrder()
                    .withOrderNumber(orderNumber)
                .then().shouldFail()
                    .errorMessage(expectedErrorMessage);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @RequiresKeycloak
    void customerShouldNotBeAbleToViewAnotherCustomersOrder() {
        scenario
                .given().order()
                    .placedByCustomer("B")
                .and().loggedInAsCustomer("A")
                .when().viewOrder()
                .then().shouldFail()
                    .errorMessage("Order " + DEFAULT_ORDER_NUMBER + " does not exist.");
    }

    @TestTemplate
    @Channel(ChannelType.API)
    @RequiresKeycloak
    void anonymousShouldNotBeAbleToViewOrder() {
        scenario
                .given().notLoggedIn()
                .when().viewOrder()
                .then().shouldFail()
                    .errorMessage(AUTHENTICATION_REQUIRED);
    }
}
