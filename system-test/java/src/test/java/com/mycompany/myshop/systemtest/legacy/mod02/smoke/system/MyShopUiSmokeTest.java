package com.mycompany.myshop.systemtest.legacy.mod02.smoke.system;

import com.mycompany.myshop.systemtest.legacy.mod02.base.BaseRawTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MyShopUiSmokeTest extends BaseRawTest {
    @BeforeEach
    void setUp() {
        setUpMyShopBrowser();
    }

    @Test
    void shouldBeAbleToGoToMyShop() {
        var response = myShopUiPage.navigate(getMyShopUiBaseUrl());

        assertThat(response.status()).isEqualTo(200);

        loginToMyShopUiIfRequired();

        var contentType = response.headers().get("content-type");
        assertThat(contentType).isNotNull().contains("text/html");

        var pageContent = myShopUiPage.content();
        assertThat(pageContent).contains("<html").contains("</html>");
    }
}


