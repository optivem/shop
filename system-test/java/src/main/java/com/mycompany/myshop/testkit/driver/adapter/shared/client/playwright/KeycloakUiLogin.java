package com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright;

import com.microsoft.playwright.Page;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.TestUser;

public class KeycloakUiLogin {
    private static final String USERNAME_SELECTOR = "#username";
    private static final String PASSWORD_SELECTOR = "#password";
    private static final String SUBMIT_SELECTOR = "#kc-login";
    /** The storefront's header link for anonymous visitors; apps that force login on every page never show it. */
    private static final String SIGN_IN_SELECTOR = "[aria-label='Sign In']";
    private static final int TIMEOUT_MILLISECONDS = 30_000;

    private final String keycloakBaseUrl;
    private final TestUser user;

    private KeycloakUiLogin(String keycloakBaseUrl, TestUser user) {
        this.keycloakBaseUrl = keycloakBaseUrl;
        this.user = user;
    }

    public static KeycloakUiLogin forBaseUrl(String keycloakBaseUrl) {
        return forBaseUrl(keycloakBaseUrl, TestUser.CUSTOMER);
    }

    public static KeycloakUiLogin forBaseUrl(String keycloakBaseUrl, TestUser user) {
        if (keycloakBaseUrl == null || keycloakBaseUrl.isBlank()) {
            return null;
        }
        return new KeycloakUiLogin(keycloakBaseUrl, user);
    }

    public TestUser getUser() {
        return user;
    }

    public boolean isLoginPage(Page page) {
        return page.url().startsWith(keycloakBaseUrl);
    }

    /** Logs in if the page is on the Keycloak login form; returns true when a login was performed. */
    public boolean loginIfRequired(Page page) {
        if (!isLoginPage(page)) {
            return false;
        }
        login(page);
        return true;
    }

    /**
     * Waits until the Keycloak login form, the app-ready selector or the "Sign In" link shows, and logs in if needed.
     * An app with an anonymous storefront shows "Sign In" instead of redirecting, so it is clicked to reach the login.
     */
    public void ensureLoggedIn(Page page, String appReadySelector) {
        page.waitForSelector(SUBMIT_SELECTOR + ", " + appReadySelector + ", " + SIGN_IN_SELECTOR,
                new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MILLISECONDS));
        if (!isLoginPage(page) && !page.isVisible(appReadySelector) && page.isVisible(SIGN_IN_SELECTOR)) {
            page.click(SIGN_IN_SELECTOR);
            // Keycloak shows its form, or sends the browser straight back when it already has a session.
            page.waitForSelector(SUBMIT_SELECTOR + ", " + appReadySelector,
                    new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MILLISECONDS));
        }
        if (isLoginPage(page)) {
            login(page);
            page.waitForSelector(appReadySelector,
                    new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MILLISECONDS));
        }
    }

    /** Waits for the login form, without filling it in. */
    public void waitForLoginForm(Page page) {
        page.waitForSelector(SUBMIT_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MILLISECONDS));
    }

    private void login(Page page) {
        page.waitForSelector(SUBMIT_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MILLISECONDS));
        page.fill(USERNAME_SELECTOR, user.getUsername());
        page.fill(PASSWORD_SELECTOR, user.getPassword());
        page.click(SUBMIT_SELECTOR);
        page.waitForURL(url -> !url.startsWith(keycloakBaseUrl),
                new Page.WaitForURLOptions().setTimeout(TIMEOUT_MILLISECONDS));
    }
}
