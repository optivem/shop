package com.optivem.eshop.systemtest.legacy.mod03.smoke.system;

import com.optivem.eshop.systemtest.legacy.mod03.base.BaseRawTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopUiSmokeTest extends BaseRawTest {
    @BeforeEach
    void setUp() {
        setUpShopBrowser();
    }

    @Test
    void shouldBeAbleToGoToShop() {
        var response = shopUiPage.navigate(getShopUiBaseUrl());

        assertEquals(200, response.status());

        var contentType = response.headers().get("content-type");
        assertTrue(contentType != null && contentType.contains("text/html"));

        var pageContent = shopUiPage.content();
        assertTrue(pageContent.contains("<html"));
        assertTrue(pageContent.contains("</html>"));
    }
}


