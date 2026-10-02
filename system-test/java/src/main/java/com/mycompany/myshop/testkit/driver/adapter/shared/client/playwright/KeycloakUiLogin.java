package com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright;

import com.microsoft.playwright.Page;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.TestUser;

public class KeycloakUiLogin {
    private static final String USERNAME_SELECTOR = "#username";
    private static final String PASSWORD_SELECTOR = "#password";
    private static final String SUBMIT_SELECTOR = "#kc-login";
    private static final int TIMEOUT_MILLISECONDS = 30_000;

    private final String keycloakBaseUrl;
    private final TestUser user;

    private KeycloakUiLogin(String keycloakBaseUrl, TestUser user) {
        this.keycloakBaseUrl = keycloakBaseUrl;
        this.user = user;
    }

    public static KeycloakUiLogin forBaseUrl(String keycloakBaseUrl) {
        if (keycloakBaseUrl == null || keycloakBaseUrl.isBlank()) {
            return null;
        }
        return new KeycloakUiLogin(keycloakBaseUrl, TestUser.ADMIN);
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

    /** Waits until either the Keycloak login form or the app-ready selector shows, logging in if needed. */
    public void ensureLoggedIn(Page page, String appReadySelector) {
        page.waitForSelector(SUBMIT_SELECTOR + ", " + appReadySelector,
                new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MILLISECONDS));
        if (isLoginPage(page)) {
            login(page);
            page.waitForSelector(appReadySelector,
                    new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MILLISECONDS));
        }
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
