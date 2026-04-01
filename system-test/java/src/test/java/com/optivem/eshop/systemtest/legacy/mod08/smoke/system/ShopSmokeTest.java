package com.optivem.eshop.systemtest.legacy.mod08.smoke.system;

import com.optivem.eshop.systemtest.legacy.mod08.base.BaseScenarioDslTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

class ShopSmokeTest extends BaseScenarioDslTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldBeAbleToGoToShop() {
        scenario.assume().shop().shouldBeRunning();
    }
}

