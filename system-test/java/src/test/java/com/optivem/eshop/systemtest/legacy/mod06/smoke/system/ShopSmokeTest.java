package com.optivem.eshop.systemtest.legacy.mod06.smoke.system;

import com.optivem.eshop.systemtest.legacy.mod06.base.BaseChannelDriverTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;

class ShopSmokeTest extends BaseChannelDriverTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldBeAbleToGoToShop() {
        var result = shopDriver.goToShop();
        assertThatResult(result).isSuccess();
    }
}

