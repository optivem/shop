package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class GiftWrapOrderPositiveTest extends BaseAcceptanceTest {
    @EnabledIfEnvironmentVariable(named = "GH_OPTIVEM_RUN_WIP_TESTS", matches = "1", disabledReason = "Work-in-progress test; set GH_OPTIVEM_RUN_WIP_TESTS=1 to run")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldMarkOrderAsGiftWrapped() {
        scenario
                .when().placeOrder()
                    .withGiftWrap()
                .then().shouldSucceed()
                .and().order()
                    .isGiftWrapped();
    }

    @EnabledIfEnvironmentVariable(named = "GH_OPTIVEM_RUN_WIP_TESTS", matches = "1", disabledReason = "Work-in-progress test; set GH_OPTIVEM_RUN_WIP_TESTS=1 to run")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldAddGiftWrappingFeeToOrderTotal() {
        scenario
                .given().product()
                    .withSku("DELL-XPS")
                    .withUnitPrice(100.00)
                .and().country()
                    .withCode("Taxfreeland")
                    .withTaxRate(0.00)
                .when().placeOrder()
                    .withSku("DELL-XPS")
                    .withQuantity(2)
                    .withCountry("Taxfreeland")
                    .withGiftWrap()
                .then().shouldSucceed()
                .and().order()
                    .hasTotalPrice(205.00);
    }
}
