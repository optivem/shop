package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class PlaceOrderShippingTest extends BaseAcceptanceTest {

    @EnabledIfEnvironmentVariable(named = "GH_OPTIVEM_RUN_WIP_TESTS", matches = "1", disabledReason = "Work-in-progress test; set GH_OPTIVEM_RUN_WIP_TESTS=1 to run")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shippingFeeShouldBeDerivedFromProductWeight() {
        scenario
                .given().product()
                    .withSku("DELL-XPS")
                    .withUnitPrice(100.00)
                    .withWeight(2.0)
                .and().country()
                    .withCode("Taxfreeland")
                    .withTaxRate(0.00)
                .when().placeOrder()
                    .withSku("DELL-XPS")
                    .withQuantity(3)
                    .withCountry("Taxfreeland")
                .then().shouldSucceed()
                .and().order()
                    .hasSubtotalPrice(300.00)
                    .hasShippingFee(0.60)
                    .hasTotalPrice(300.60);
    }

    @EnabledIfEnvironmentVariable(named = "GH_OPTIVEM_RUN_WIP_TESTS", matches = "1", disabledReason = "Work-in-progress test; set GH_OPTIVEM_RUN_WIP_TESTS=1 to run")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void lighterProductShouldYieldLowerShippingFee() {
        scenario
                .given().product()
                    .withSku("HP15")
                    .withUnitPrice(50.00)
                    .withWeight(0.5)
                .and().country()
                    .withCode("Taxfreeland")
                    .withTaxRate(0.00)
                .when().placeOrder()
                    .withSku("HP15")
                    .withQuantity(2)
                    .withCountry("Taxfreeland")
                .then().shouldSucceed()
                .and().order()
                    .hasSubtotalPrice(100.00)
                    .hasShippingFee(0.10)
                    .hasTotalPrice(100.10);
    }
}
