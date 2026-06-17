package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class ErpProductWeightTest extends BaseAcceptanceTest {

    @EnabledIfEnvironmentVariable(named = "GH_OPTIVEM_RUN_WIP_TESTS", matches = "1", disabledReason = "Work-in-progress test; set GH_OPTIVEM_RUN_WIP_TESTS=1 to run")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void erpShouldReturnProductWeight() {
        scenario
                .given().product()
                    .withSku("SKU-123")
                    .withUnitPrice(12.00)
                    .withWeight(1.5)
                .then().product("SKU-123")
                    .hasPrice(12.00)
                    .hasWeight(1.5);
    }
}
