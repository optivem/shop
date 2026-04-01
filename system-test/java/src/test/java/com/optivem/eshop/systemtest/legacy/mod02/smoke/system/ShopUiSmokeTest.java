package com.optivem.eshop.systemtest.legacy.mod02.smoke.system;

import com.optivem.eshop.systemtest.legacy.mod02.base.BaseRawTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShopUiSmokeTest extends BaseRawTest {
    @BeforeEach
    void setUp() {
        setUpShopBrowser();
    }

    @Test
    void shouldBeAbleToGoToShop() {
        var response = shopUiPage.navigate(getShopUiBaseUrl());

        assertThat(response.status()).isEqualTo(200);

        var contentType = response.headers().get("content-type");
        assertThat(contentType).isNotNull().contains("text/html");

        var pageContent = shopUiPage.content();
        assertThat(pageContent).contains("<html").contains("</html>");
    }
}


