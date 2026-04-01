package com.optivem.eshop.systemtest.legacy.mod07.smoke.system;

import com.optivem.eshop.systemtest.legacy.mod07.base.BaseUseCaseDslTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

class ShopSmokeTest extends BaseUseCaseDslTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldBeAbleToGoToShop() {
        app.shop().goToShop()
                .execute()
                .shouldSucceed();
    }
}

