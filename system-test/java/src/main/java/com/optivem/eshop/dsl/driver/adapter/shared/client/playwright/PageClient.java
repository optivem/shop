package com.optivem.eshop.dsl.driver.adapter.shared.client.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.List;

public class PageClient {
    // Increased default timeout for parallel test execution
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_TIMEOUT_MILLISECONDS = DEFAULT_TIMEOUT_SECONDS * 1000;
    private final Page page;
    private final int timeoutMilliseconds;

    public PageClient(Page page) {
        this(page, DEFAULT_TIMEOUT_MILLISECONDS);
    }

    public PageClient(Page page, int timeoutMilliseconds) {
        this.page = page;
        this.timeoutMilliseconds = timeoutMilliseconds;
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

    public List<String> readAllTextContents(String selector) {
        var locator = page.locator(selector);
        // Wait for at least one element to be visible
        // allTextContents() doesn't trigger strict mode - it's designed for multiple elements
        locator.first().waitFor(getDefaultWaitForOptions());
        return locator.allTextContents();
    }

    public void waitForVisible(String selector) {
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

    public void removeElements(String selector) {
        page.evaluate("selector => document.querySelectorAll(selector).forEach(el => el.remove())", selector);
    }

    public boolean isHidden(String selector) {
        var locator = page.locator(selector);
        return locator.count() == 0;
    }

    private Locator getLocator(String selector, Locator.WaitForOptions waitForOptions) {
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


