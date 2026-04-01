package com.optivem.eshop.systemtest.legacy.mod10.acceptance;

import com.optivem.eshop.systemtest.legacy.mod10.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.testing.Channel;
import com.optivem.testing.DataSource;

import org.junit.jupiter.api.TestTemplate;

class PlaceOrderPositiveTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldBeAbleToPlaceOrderForValidInput() {
        scenario
                .given().product()
                    .withSku("ABC")
                    .withUnitPrice(20.00)
                .when().placeOrder()
                    .withSku("ABC")
                    .withQuantity(5)
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void orderStatusShouldBePlacedAfterPlacingOrder() {
        scenario
                .when().placeOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasStatus(OrderStatus.PLACED);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldCalculateBasePriceAsProductOfUnitPriceAndQuantity() {
        scenario
                .given().product()
                    .withUnitPrice(20.00)
                .when().placeOrder()
                    .withQuantity(5)
                .then().shouldSucceed()
                .and().order()
                    .hasTotalPrice(100.00);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @DataSource({"20.00", "5", "100.00"})
    @DataSource({"10.00", "3", "30.00"})
    @DataSource({"15.50", "4", "62.00"})
    @DataSource({"99.99", "1", "99.99"})
    void shouldPlaceOrderWithCorrectBasePriceParameterized(String unitPrice, String quantity, String basePrice) {
        scenario
                .given().product()
                    .withUnitPrice(unitPrice)
                .when().placeOrder()
                    .withQuantity(quantity)
                .then().shouldSucceed()
                .and().order()
                    .hasTotalPrice(basePrice);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void orderPrefixShouldBeORD() {
        scenario
                .when().placeOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasOrderNumberPrefix("ORD-");
    }

    // TODO: Place order for exact available quantity
}




