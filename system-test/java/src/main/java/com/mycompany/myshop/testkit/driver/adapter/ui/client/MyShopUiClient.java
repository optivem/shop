package com.mycompany.myshop.testkit.driver.adapter.ui.client;

import com.microsoft.playwright.*;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.CartPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.CatalogPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.CheckoutPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.CouponManagementPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.HomePage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.NewOrderPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.OrderDetailsPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.ProductPage;
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
    private static final String PRODUCT_PATH = "/products/";
    private static final String CART_PATH = "/cart";
    private static final String CHECKOUT_PATH = "/checkout";
    private static final String PROCEED_TO_CHECKOUT_SELECTOR = "a[aria-label='Proceed to Checkout']";
    private static final String COUPON_MANAGEMENT_PATH = "/admin-coupons";
    private static final String CONTENT_TYPE = "content-type";
    private static final String TEXT_HTML = "text/html";
    private static final String HTML_OPENING_TAG = "<html";
    private static final String HTML_CLOSING_TAG = "</html>";

    private static final String HOME_READY_SELECTOR = "a[href='/order-history']";
    private static final String SINGLE_PAGE_APP_ROOT_SELECTOR = "#root";
    private static final String LOGIN_FORM_SELECTOR = "#kc-login";

    private final String baseUrl;
    private final Browser browser;
    private final String keycloakBaseUrl;
    private BrowserContext context;
    private Page page;
    private PageClient pageClient;
    private HomePage homePage;
    private KeycloakUiLogin login;
    /** Set while the browser session is a visitor's, which never logs in. */
    private PageClient visitorPageClient;

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
        if (login == null || login.getUser().equals(user)) {
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
        this.visitorPageClient = null;
        this.response = null;
    }

    /** Replaces the browser session with a fresh one that never logs in: the anonymous visitor's. */
    public void startVisitorSession() {
        close();
        open(login == null ? TestUser.CUSTOMER : login.getUser());
        visitorPageClient = new PageClient(page);
    }

    /**
     * Opens a product by its page address, in the visitor's session if one was started, else as the logged-in user.
     *
     * @return the product page, or null when the app sends the visitor to the login form instead
     */
    public ProductPage openProductPage(String sku) {
        var path = PRODUCT_PATH + URLEncoder.encode(sku, StandardCharsets.UTF_8).replace("+", "%20");
        if (visitorPageClient != null) {
            response = page.navigate(baseUrl + path);
            visitorPageClient.waitForVisible(ProductPage.READY_SELECTOR + ", " + LOGIN_FORM_SELECTOR);
            if (login != null && login.isLoginPage(page)) {
                return null;
            }
            return new ProductPage(visitorPageClient);
        }
        openHomePage();
        openDirectly(path, path);
        return new ProductPage(pageClient);
    }

    /**
     * Opens the catalog in the visitor's session if one was started, else as the logged-in user.
     *
     * @return the catalog, or null when the app sends the visitor to the login form instead
     */
    public CatalogPage openCatalogInSession() {
        if (visitorPageClient == null) {
            return openCatalog();
        }
        response = page.navigate(baseUrl);
        visitorPageClient.waitForVisible(CatalogPage.READY_SELECTOR + ", " + LOGIN_FORM_SELECTOR);
        if (login != null && login.isLoginPage(page)) {
            return null;
        }
        return new CatalogPage(visitorPageClient);
    }

    /**
     * Leaves the site in the same browser: the page is closed and a new, blank one takes its place, keeping the
     * browser's cookies and storage. Open a page to come back.
     */
    public void revisit() {
        Closer.close(page);
        page = context.newPage();
        pageClient = new PageClient(page, login);
        homePage = new HomePage(pageClient);
        if (visitorPageClient != null) {
            visitorPageClient = new PageClient(page);
        }
    }

    /** Signs in as {@code user} with the header's "Sign In" link, from the page that is open. */
    public void signIn(TestUser user) {
        var userLogin = KeycloakUiLogin.forBaseUrl(keycloakBaseUrl, user);
        if (userLogin == null) {
            throw new IllegalStateException("Signing in can only be exercised against a system with a login");
        }
        userLogin.ensureLoggedIn(page, HOME_READY_SELECTOR);
    }

    /** Whether the page is the login form. */
    public boolean isOnLoginPage() {
        return login != null && login.isLoginPage(page);
    }

    /** The page that is open, read for what every page shows, such as the header's cart badge. */
    public HomePage currentPageInSession() {
        return new HomePage(visitorPageClient != null ? visitorPageClient : pageClient);
    }

    /** The page client of the current session: the visitor's, if one was started, else the logged-in user's. */
    public PageClient sessionPageClient() {
        return visitorPageClient != null ? visitorPageClient : pageClient;
    }

    /**
     * Opens the checkout by its page address in the current session, and waits until the app shows where it sent the
     * user: the checkout, the cart, the login form or a refusal.
     */
    public void openCheckoutPage() {
        if (visitorPageClient != null) {
            response = page.navigate(baseUrl + CHECKOUT_PATH);
        } else {
            openHomePage();
            openDirectly(CHECKOUT_PATH, CHECKOUT_PATH);
        }
        waitForCheckoutOutcome();
    }

    /** Opens the cart in the current session and follows its "Proceed to checkout", like {@link #openCheckoutPage()}. */
    public void proceedToCheckoutFromCart() {
        var cartPage = openCartPage();
        cartPage.waitForLoaded();
        sessionPageClient().click(PROCEED_TO_CHECKOUT_SELECTOR);
        waitForCheckoutOutcome();
    }

    /** Signs in as {@code user} on the login form that is open, and waits for the page the app returns to. */
    public void signInOnLoginForm(TestUser user) {
        var userLogin = KeycloakUiLogin.forBaseUrl(keycloakBaseUrl, user);
        if (userLogin == null) {
            throw new IllegalStateException("Signing in can only be exercised against a system with a login");
        }
        userLogin.ensureLoggedIn(page, CheckoutPage.READY_SELECTOR + ", " + CartPage.READY_SELECTOR + ", " + CatalogPage.READY_SELECTOR);
        // A single-page UI may show its (public) landing page while it still exchanges the login code, and only then
        // move to the page the visitor came from: wait until the code is gone from the address.
        page.waitForURL(url -> !url.matches(".*[?&]code=.*"));
        if (page.locator(CheckoutPage.READY_SELECTOR).count() == 0 && page.url().contains(CHECKOUT_PATH)) {
            sessionPageClient().waitForVisible(CheckoutPage.READY_SELECTOR);
        }
    }

    /** Whether the app refused the page: a 403 response (server-rendered UIs) or a "Not authorized" page (React). */
    public boolean isRefused() {
        return (response != null && response.status() == HttpStatus.FORBIDDEN.value() && page.url().contains(CHECKOUT_PATH))
                || page.locator(NOT_AUTHORIZED_SELECTOR).count() > 0;
    }

    /** The path of the page address that is open, such as "/cart". */
    public String currentPath() {
        return java.net.URI.create(page.url()).getPath();
    }

    private void waitForCheckoutOutcome() {
        if (response != null && response.status() == HttpStatus.FORBIDDEN.value() && page.url().contains(CHECKOUT_PATH)) {
            return;
        }
        page.waitForSelector(CheckoutPage.READY_SELECTOR + ", " + CartPage.READY_SELECTOR + ", " + LOGIN_FORM_SELECTOR
                + ", " + NOT_AUTHORIZED_SELECTOR);
    }

    /** Opens the cart in the current session. The app must already be open. */
    public CartPage openCartPage() {
        if (visitorPageClient != null) {
            response = page.navigate(baseUrl + CART_PATH);
            return new CartPage(visitorPageClient);
        }
        openDirectly(CART_PATH, CART_PATH);
        return new CartPage(pageClient);
    }

    public HomePage openHomePage() {
        response = page.navigate(baseUrl);
        if (login != null) {
            login.ensureLoggedIn(page, HOME_READY_SELECTOR);
        }
        return homePage;
    }

    /** Opens the catalog, the home page, as the logged-in user. */
    public CatalogPage openCatalog() {
        openHomePage();
        return new CatalogPage(pageClient);
    }

    /**
     * Opens the catalog in a fresh session that never logs in.
     *
     * @return the catalog, or null when the app sends the visitor to the login form instead
     */
    public CatalogPage openCatalogAnonymously() {
        close();
        open(login == null ? TestUser.CUSTOMER : login.getUser());
        response = page.navigate(baseUrl);
        var anonymousPageClient = new PageClient(page);
        anonymousPageClient.waitForVisible(CatalogPage.READY_SELECTOR + ", " + LOGIN_FORM_SELECTOR);
        if (login != null && login.isLoginPage(page)) {
            return null;
        }
        return new CatalogPage(anonymousPageClient);
    }

    /** Opens an order by its URL, not through the order history. The app must already be open. */
    public OrderDetailsPage openOrderDetailsPage(String orderNumber) {
        var encodedOrderNumber = URLEncoder.encode(orderNumber, StandardCharsets.UTF_8).replace("+", "%20");
        openDirectly("/order-details?orderNumber=" + encodedOrderNumber, "/order-details/" + encodedOrderNumber);
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
        assertLoginRequiredFor(path, path);
    }

    /**
     * Like {@link #assertLoginRequiredFor(String)}, for a page whose React route differs from its server-rendered path:
     * the React frontend matches no route for the server path, so it would never ask to log in.
     */
    public void assertLoginRequiredFor(String serverPath, String singlePageAppRoute) {
        if (login == null) {
            throw new IllegalStateException("Anonymous access can only be exercised against a system with a login");
        }
        try (var anonymousContext = browser.newContext()) {
            var anonymousPage = anonymousContext.newPage();
            anonymousPage.navigate(baseUrl + serverPath);
            if (!singlePageAppRoute.equals(serverPath) && anonymousPage.locator(SINGLE_PAGE_APP_ROOT_SELECTOR).count() > 0) {
                anonymousPage.navigate(baseUrl + singlePageAppRoute);
            }
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
