package com.optivem.eshop.systemtest.legacy.mod04.smoke.system;

import com.optivem.eshop.systemtest.legacy.mod04.base.BaseClientTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopUiSmokeTest extends BaseClientTest {
    @BeforeEach
    void setUp() {
        setUpShopUiClient();
    }

    @Test
    void shouldBeAbleToGoToShop() {
        shopUiClient.openHomePage();
        assertTrue(shopUiClient.isPageLoaded());
    }
}


