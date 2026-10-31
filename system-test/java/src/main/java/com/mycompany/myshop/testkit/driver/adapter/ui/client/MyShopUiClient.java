package com.mycompany.myshop.testkit.driver.adapter.ui.client;

import com.microsoft.playwright.*;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.CouponManagementPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.HomePage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.NewOrderPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.OrderDetailsPage;
import com.mycompany.myshop.testkit.common.Closer;
import com.mycompany.myshop.testkit.common.Result;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.TestUser;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.KeycloakUiLogin;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.PageClient;
import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;
import org.springframework.http.HttpStatus;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.mycompany.myshop.testkit.dsl.core.usecase.commons.SystemResults.failure;
import static com.mycompany.myshop.testkit.dsl.core.usecase.commons.SystemResults.success;

public class MyShopUiClient implements AutoCloseable {
    public static final String AUTHENTICATION_REQUIRED = "Authentication is required";

    public static final String PERMISSION_DENIED = "You do not have permission to perform this action";
    private static final String NOT_AUTHORIZED_SELECTOR = "h1:has-text('Not authorized'):visible";
    private static final String NEW_ORDER_PATH = "/new-order";
    private static final String COUPON_MANAGEMENT_PATH = "/admin-coupons";
    private static final String CONTENT_TYPE = "content-type";
    private static final String TEXT_HTML = "text/html";
    private static final String HTML_OPENING_TAG = "<html";
    private static final String HTML_CLOSING_TAG = "</html>";

    private static final String HOME_READY_SELECTOR = "a[href='/order-history']";
    private static final String SINGLE_PAGE_APP_ROOT_SELECTOR = "#root";

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

    /** Opens an order by its URL, not through the order history. The app must already be open. */
    public OrderDetailsPage openOrderDetailsPage(String orderNumber) {
        var encodedOrderNumber = URLEncoder.encode(orderNumber, StandardCharsets.UTF_8).replace("+", "%20");
        var path = "/order-details/" + encodedOrderNumber;
        openDirectly(path, path);
        return new OrderDetailsPage(pageClient);
    }

    /** Opens the new order page by its URL, reporting a refusal. The app must already be open. */
    public Result<NewOrderPage, SystemError> openNewOrderPage() {
        return openRestrictedPage(NEW_ORDER_PATH, NewOrderPage.READY_SELECTOR, new NewOrderPage(pageClient));
    }

    /** Opens the coupon management page by its URL, reporting a refusal. The app must already be open. */
    public Result<CouponManagementPage, SystemError> openCouponManagementPage() {
        return openRestrictedPage(COUPON_MANAGEMENT_PATH, CouponManagementPage.READY_SELECTOR, new CouponManagementPage(pageClient));
    }

    /** Opens {@code path} in a throwaway session that never logs in, and requires the app to send it to the login form. */
    public void assertLoginRequiredFor(String path) {
        if (login == null) {
            throw new IllegalStateException("Anonymous access can only be exercised against a system with a login");
        }
        try (var anonymousContext = browser.newContext()) {
            var anonymousPage = anonymousContext.newPage();
            anonymousPage.navigate(baseUrl + path);
            login.waitForLoginForm(anonymousPage);
        }
    }

    /** The server-rendered UIs refuse with a 403 response; the React frontend renders a "Not authorized" page. */
    private <T> Result<T, SystemError> openRestrictedPage(String path, String readySelector, T openedPage) {
        openDirectly(path, path);

        if (response != null && response.status() == HttpStatus.FORBIDDEN.value()) {
            return failure(PERMISSION_DENIED);
        }

        pageClient.waitForVisible(readySelector + ", " + NOT_AUTHORIZED_SELECTOR);
        return pageClient.isHidden(NOT_AUTHORIZED_SELECTOR) ? success(openedPage) : failure(PERMISSION_DENIED);
    }

    /**
     * Server-rendered UIs are simply navigated to. The React frontend keeps its session in memory and returns to
     * the home page after the login redirect, so a reload would lose the URL: it is moved to the route in place.
     */
    private void openDirectly(String serverPath, String singlePageAppRoute) {
        if (page.locator(SINGLE_PAGE_APP_ROOT_SELECTOR).count() > 0) {
            pageClient.navigateWithinApp(singlePageAppRoute);
        } else {
            response = page.navigate(baseUrl + serverPath);
        }
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
