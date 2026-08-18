package com.mycompany.myshop.backend.component.latest.smoke.system;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

class MyShopSmokeTest extends BaseComponentTest {

    @Test
    void shouldBeAbleToGoToMyShop() {
        scenario.assume().myShop().shouldBeRunning();
    }
}
