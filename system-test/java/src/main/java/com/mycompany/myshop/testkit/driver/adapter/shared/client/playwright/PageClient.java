package com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.List;

public class PageClient {
    // Increased default timeout for parallel test execution
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_TIMEOUT_MILLISECONDS = DEFAULT_TIMEOUT_SECONDS * 1000;
    private final Page page;
    private final int timeoutMilliseconds;
    private final KeycloakUiLogin login;

    public PageClient(Page page) {
        this(page, DEFAULT_TIMEOUT_MILLISECONDS, null);
    }

    public PageClient(Page page, KeycloakUiLogin login) {
        this(page, DEFAULT_TIMEOUT_MILLISECONDS, login);
    }

    public PageClient(Page page, int timeoutMilliseconds) {
        this(page, timeoutMilliseconds, null);
    }

    public PageClient(Page page, int timeoutMilliseconds, KeycloakUiLogin login) {
        this.page = page;
        this.timeoutMilliseconds = timeoutMilliseconds;
        this.login = login;
    }

    private void loginIfRedirected() {
        if (login != null) {
            login.loginIfRequired(page);
        }
    }

    public void fill(String selector, String value) {
        var locator = getLocator(selector);
        var processedValue = value == null ? "" : value;
        locator.fill(processedValue);
    }

    public void click(String selector) {
        var locator = getLocator(selector);
        locator.click();
    }

    public String readTextContent(String selector) {
        var locator = getLocator(selector);
        return locator.textContent();
    }

    public String readAttribute(String selector, String attribute) {
        var locator = getLocator(selector);
        return locator.getAttribute(attribute);
    }

    public String readInputValue(String selector) {
        var locator = getLocator(selector);
        return locator.inputValue();
    }

    public List<String> readAllTextContents(String selector) {
        var locator = page.locator(selector);
        loginIfRedirected();
        // Wait for at least one element to be visible
        // allTextContents() doesn't trigger strict mode - it's designed for multiple elements
        locator.first().waitFor(getDefaultWaitForOptions());
        return locator.allTextContents();
    }

    public List<String> readAllTextContentsNoWait(String selector) {
        var locator = page.locator(selector);
        return locator.allTextContents();
    }

    public void waitForVisible(String selector) {
        loginIfRedirected();
        var locator = page.locator(selector);
        locator.waitFor(getDefaultWaitForOptions());
    }

    public boolean isVisible(String selector) {
        try {
            var locator = getLocator(selector);
            return locator.count() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Moves a single-page app to a route without reloading it. */
    public void navigateWithinApp(String route) {
        page.evaluate("route => { history.pushState({}, '', route); dispatchEvent(new PopStateEvent('popstate')); }", route);
    }

    /** Reads an attribute of every element the selector matches, without waiting for any. */
    public List<String> readAllAttributesNoWait(String selector, String attribute) {
        return page.locator(selector).all().stream().map(locator -> locator.getAttribute(attribute)).toList();
    }

    public void selectOptionByLabel(String selector, String label) {
        getLocator(selector).selectOption(new SelectOption().setLabel(label));
    }

    /** Selects the option with this value; a value the select does not offer is added first, as an edited form would send it. */
    public void selectOptionByValueAddingIfMissing(String selector, String value) {
        var locator = getLocator(selector);
        locator.evaluate("(select, value) => { if (![...select.options].some(o => o.value === value)) {"
                + " const option = document.createElement('option'); option.value = value; option.text = value; select.add(option); } }", value);
        locator.selectOption(value);
    }

    public void setChecked(String selector, boolean checked) {
        getLocator(selector).setChecked(checked);
    }

    public boolean isChecked(String selector) {
        return getLocator(selector).isChecked();
    }

    public String readSelectedOptionLabel(String selector) {
        return (String) getLocator(selector).evaluate("select => select.options[select.selectedIndex]?.text ?? null");
    }

    public String currentUrl() {
        return page.url();
    }

    /** Waits until the page address differs from {@code previousUrl}: a page load or an in-app route change. */
    public void waitForUrlChange(String previousUrl) {
        page.waitForURL(url -> !url.equals(previousUrl), new Page.WaitForURLOptions().setTimeout(timeoutMilliseconds));
    }

    /** Opens the current page address again, as a fresh page load. */
    public void reopenCurrentUrl() {
        page.navigate(page.url());
    }

    public void press(String selector, String key) {
        getLocator(selector).press(key);
    }

    /** Whether the element belongs to a form, so acting on it posts the form and loads a new document. */
    public boolean belongsToForm(String selector) {
        return Boolean.TRUE.equals(getLocator(selector).evaluate("element => element.form != null"));
    }

    /** Runs an action that loads a new document (such as a form post), and waits until the new document is ready. */
    public void runAndWaitForNewDocument(Runnable action) {
        page.evaluate("() => { window.__previousDocument = true; }");
        action.run();
        page.waitForFunction("() => window.__previousDocument === undefined && document.readyState !== 'loading'", null,
                new Page.WaitForFunctionOptions().setTimeout(timeoutMilliseconds));
    }

    public void removeElements(String selector) {
        page.evaluate("selector => document.querySelectorAll(selector).forEach(el => el.remove())", selector);
    }

    public boolean isHidden(String selector) {
        var locator = page.locator(selector);
        return locator.count() == 0;
    }

    private Locator getLocator(String selector, Locator.WaitForOptions waitForOptions) {
        loginIfRedirected();
        var locator = page.locator(selector);
        locator.waitFor(waitForOptions);

        if (locator.count() == 0) {
            throw new IllegalStateException("No elements found for selector: " + selector);
        }

        return locator;
    }

    private Locator getLocator(String selector) {
        var waitForOptions = getDefaultWaitForOptions();
        return getLocator(selector, waitForOptions);
    }

    private Locator.WaitForOptions getDefaultWaitForOptions() {
        return new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(timeoutMilliseconds);
    }
}
