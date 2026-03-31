package com.optivem.eshop.systemtest.infrastructure.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.extension.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Proper JUnit 5 extension that manages browser lifecycle.
 * Creates one browser per worker thread (for parallel execution safety).
 * Uses ThreadLocal to make browser accessible to code without ExtensionContext.
 *
 * Usage in test class:
 * @ExtendWith(BrowserLifecycleExtension.class)
 *
 * Usage in code:
 * Browser browser = BrowserLifecycleExtension.getBrowser();
 */
public class BrowserLifecycleExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    // One browser per worker thread for parallel execution safety
    private static final Map<Long, BrowserResource> BROWSERS_BY_THREAD = new ConcurrentHashMap<>();
    private static final ThreadLocal<Browser> THREAD_LOCAL_BROWSER = new ThreadLocal<>();

    private static final AtomicInteger ACTIVE_TEST_CLASSES = new AtomicInteger(0);

    private static final boolean IS_HEADLESS = getHeadlessMode();
    private static final int SLOW_MO_MS = 100;
    private static final boolean DEFAULT_HEADLESS = true;

    private static boolean getHeadlessMode() {
        String systemProperty = System.getProperty("headless");
        if (systemProperty != null) {
            return Boolean.parseBoolean(systemProperty);
        }

        String envVariable = System.getenv("HEADED");
        if (envVariable != null) {
            return !Boolean.parseBoolean(envVariable);
        }

        if (isRunningInCI()) {
            return true;
        }

        return DEFAULT_HEADLESS;
    }

    private static boolean isRunningInCI() {
        return System.getenv("CI") != null ||
               System.getenv("JENKINS_HOME") != null ||
               System.getenv("GITHUB_ACTIONS") != null ||
               System.getenv("GITLAB_CI") != null ||
               System.getenv("CIRCLECI") != null ||
               System.getenv("TRAVIS") != null ||
               System.getenv("TEAMCITY_VERSION") != null ||
               System.getenv("BUILDKITE") != null;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        ACTIVE_TEST_CLASSES.incrementAndGet();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        int count = ACTIVE_TEST_CLASSES.decrementAndGet();

        // Clean up all browsers when last test class finishes
        if (count == 0) {
            BROWSERS_BY_THREAD.values().forEach(BrowserResource::close);
            BROWSERS_BY_THREAD.clear();
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        long threadId = Thread.currentThread().threadId();

        // Create browser for this thread if not already exists (lazy initialization)
        BROWSERS_BY_THREAD.computeIfAbsent(threadId, id -> new BrowserResource());

        // Set browser in ThreadLocal for this test
        Browser browser = getBrowser();
        THREAD_LOCAL_BROWSER.set(browser);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // Clean up ThreadLocal
        THREAD_LOCAL_BROWSER.remove();
    }

    /**
     * Get browser for current thread.
     *
     * @return Browser instance for current thread
     * @throws IllegalStateException if called outside test context
     */
    public static Browser getBrowser() {
        Browser browser = THREAD_LOCAL_BROWSER.get();
        if (browser != null) {
            return browser;
        }

        // Fallback: get from thread map
        long threadId = Thread.currentThread().threadId();
        BrowserResource resource = BROWSERS_BY_THREAD.get(threadId);
        if (resource != null) {
            return resource.getBrowser();
        }

        throw new IllegalStateException(
            "Browser not available. Ensure @ExtendWith(BrowserLifecycleExtension.class) is present on test class."
        );
    }

    /**
     * Resource wrapper that manages browser lifecycle.
     */
    private static class BrowserResource {
        private final Playwright playwright;
        private final Browser browser;

        public BrowserResource() {
            this.playwright = Playwright.create();

            var launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(IS_HEADLESS)
                    .setSlowMo(SLOW_MO_MS);

            this.browser = playwright.chromium().launch(launchOptions);
        }

        public Browser getBrowser() {
            return browser;
        }

        public void close() {
            if (browser != null) {
                try {
                    browser.close();
                } catch (Exception e) {
                    // Ignore - browser cleanup best effort
                }
            }
            if (playwright != null) {
                try {
                    playwright.close();
                } catch (Exception e) {
                    // Ignore - playwright cleanup best effort
                }
            }
        }
    }
}

