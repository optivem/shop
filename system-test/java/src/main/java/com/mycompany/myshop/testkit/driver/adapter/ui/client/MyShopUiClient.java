package com.mycompany.myshop.testkit.driver.adapter.ui.client;

import com.microsoft.playwright.*;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.HomePage;
import com.mycompany.myshop.testkit.common.Closer;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.TestUser;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.KeycloakUiLogin;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.PageClient;
import org.springframework.http.HttpStatus;

public class MyShopUiClient implements AutoCloseable {
    private static final String CONTENT_TYPE = "content-type";
    private static final String TEXT_HTML = "text/html";
    private static final String HTML_OPENING_TAG = "<html";
    private static final String HTML_CLOSING_TAG = "</html>";

    private static final String HOME_READY_SELECTOR = "a[href='/order-history']";

    private final String baseUrl;
    private final Browser browser;
    private final String keycloakBaseUrl;
    private BrowserContext context;
    private Page page;
    private PageClient pageClient;
    private HomePage homePage;
    private KeycloakUiLogin login;

    private Response response;

    public MyShopUiClient(String baseUrl, Browser browser) {
        this(baseUrl, browser, null);
    }

    public MyShopUiClient(String baseUrl, Browser browser, String keycloakBaseUrl) {
        this.baseUrl = baseUrl;
        this.browser = browser;
        this.keycloakBaseUrl = keycloakBaseUrl;
        open(TestUser.CUSTOMER);
    }

    /**
     * Makes {@code user} the logged-in user. Switching discards the browser session (a fresh isolated
     * context) so the next page open logs in as the new user. No-op without Keycloak or when already that user.
     *
     * @return true when the session was replaced, so callers must re-open the home page
     */
    public boolean switchUser(TestUser user) {
        if (login == null || login.getUser() == user) {
            return false;
        }
        close();
        open(user);
        return true;
    }

    private void open(TestUser user) {
        this.login = KeycloakUiLogin.forBaseUrl(keycloakBaseUrl, user);

        // Create isolated browser context for this test instance
        var contextOptions = new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                // Ensure complete isolation between parallel tests
                .setStorageStatePath(null);

        this.context = browser.newContext(contextOptions);

        // Each test gets its own page
        this.page = context.newPage();

        this.pageClient = new PageClient(page, login);
        this.homePage = new HomePage(pageClient);
        this.response = null;
    }

    public HomePage openHomePage() {
        response = page.navigate(baseUrl);
        if (login != null) {
            login.ensureLoggedIn(page, HOME_READY_SELECTOR);
        }
        return homePage;
    }

    public boolean isStatusOk() {
        return response.status() == HttpStatus.OK.value();
    }

    public boolean isPageLoaded() {
        if (response == null || response.status() != HttpStatus.OK.value()) {
            return false;
        }

        var contentType = response.headers().get(CONTENT_TYPE);
        if (contentType == null || !contentType.startsWith(TEXT_HTML)) {
            return false;
        }

        var pageContent = page.content();
        return pageContent != null && pageContent.contains(HTML_OPENING_TAG) && pageContent.contains(HTML_CLOSING_TAG);
    }

    @Override
    public void close() {
        Closer.close(page);
        Closer.close(context);
        // Don't close browser - it's shared and managed by test lifecycle infrastructure
    }
}
