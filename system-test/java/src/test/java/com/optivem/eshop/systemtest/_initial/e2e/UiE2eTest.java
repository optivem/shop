package com.optivem.eshop.systemtest._initial.e2e;

import com.microsoft.playwright.*;
import com.optivem.eshop.systemtest.configuration.ConfigurationLoader;
import com.optivem.eshop.systemtest.configuration.PropertyLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UiE2eTest {

    @Test
    void fetchTodo_shouldDisplayTodoDataInUI() {
        var environment = PropertyLoader.getEnvironment(null);
        var externalSystemMode = PropertyLoader.getExternalSystemMode(null);
        var config = ConfigurationLoader.load(environment, externalSystemMode);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();

            page.navigate(config.getShopUiBaseUrl() + "/todos");

            Locator todoIdInput = page.locator("#todoId");
            assertTrue(todoIdInput.isVisible(), "Todo ID input textbox should be visible");

            todoIdInput.fill("4");

            Locator fetchButton = page.locator("#fetchTodo");
            fetchButton.click();

            Locator todoResult = page.locator("#todoResult");
            todoResult.waitFor(new Locator.WaitForOptions().setTimeout(5000));
            page.waitForTimeout(3000);

            String resultText = todoResult.textContent();

            assertTrue(resultText.contains("ID") && (resultText.contains("4") || resultText.contains(": 4")),
                      "Result should contain 'ID: 4'. Actual text: " + resultText);
            assertTrue(resultText.contains("User ID") && (resultText.contains("1") || resultText.contains(": 1")),
                      "Result should contain 'User ID: 1'. Actual text: " + resultText);
            assertTrue(resultText.contains("Title"),
                      "Result should contain 'Title' field. Actual text: " + resultText);
            assertTrue(resultText.contains("Completed") && (resultText.contains("Yes") || resultText.contains("No")),
                      "Result should contain 'Completed' field with value 'Yes' or 'No'. Actual text: " + resultText);

            browser.close();
        }
    }
}
