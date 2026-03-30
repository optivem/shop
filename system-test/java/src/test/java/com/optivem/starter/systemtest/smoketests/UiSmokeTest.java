package com.optivem.starter.systemtest.smoketests;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiSmokeTest {

    @Test
    void home_shouldReturnHtmlContent() {
        // DISCLAIMER: This is an example of a badly written test
        // which unfortunately simulates real-life software test projects.
        // This is the starting point for our Starter exercises.

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();

            // Navigate and get response
            Response response = page.navigate("http://localhost:8080/");

            // Assert
            assertEquals(200, response.status());

            // Check content type is HTML
            String contentType = response.headers().get("content-type");
            assertTrue(contentType != null && contentType.contains("text/html"),
                      "Content-Type should be text/html, but was: " + contentType);

            // Check HTML structure using Playwright's content method
            String pageContent = page.content();
            assertTrue(pageContent.contains("<html"), "Response should contain HTML opening tag");
            assertTrue(pageContent.contains("</html>"), "Response should contain HTML closing tag");

            browser.close();
        }
    }
}
