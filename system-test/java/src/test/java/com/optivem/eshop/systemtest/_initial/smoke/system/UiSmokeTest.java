package com.optivem.eshop.systemtest._initial.smoke.system;

import com.microsoft.playwright.*;
import com.optivem.eshop.systemtest.configuration.ConfigurationLoader;
import com.optivem.eshop.systemtest.configuration.PropertyLoader;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiSmokeTest {

    @Test
    void home_shouldReturnHtmlContent() {
        var environment = PropertyLoader.getEnvironment(null);
        var externalSystemMode = PropertyLoader.getExternalSystemMode(null);
        var config = ConfigurationLoader.load(environment, externalSystemMode);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();

            Response response = page.navigate(config.getShopUiBaseUrl() + "/");

            assertEquals(200, response.status());

            String contentType = response.headers().get("content-type");
            assertTrue(contentType != null && contentType.contains("text/html"),
                      "Content-Type should be text/html, but was: " + contentType);

            String pageContent = page.content();
            assertTrue(pageContent.contains("<html"), "Response should contain HTML opening tag");
            assertTrue(pageContent.contains("</html>"), "Response should contain HTML closing tag");

            browser.close();
        }
    }
}
