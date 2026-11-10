import type { Page } from '@playwright/test';
import { TestUsers, type TestUser } from '../http/test-user.js';

const USERNAME_SELECTOR = '#username';
const PASSWORD_SELECTOR = '#password';
const SUBMIT_SELECTOR = '#kc-login';
/** The storefront's header link for anonymous visitors; apps that force login on every page never show it. */
const SIGN_IN_SELECTOR = "[aria-label='Sign In']";
const TIMEOUT_MS = 30_000;

/** Logs the browser in on the Keycloak login form when the app redirects there. */
export class KeycloakUiLogin {
  private constructor(
    private readonly keycloakBaseUrl: string,
    private readonly user: TestUser,
  ) {}

  /** Returns undefined when no Keycloak URL is configured (the system under test has no login). */
  static forBaseUrl(keycloakBaseUrl: string | undefined, user: TestUser = TestUsers.CUSTOMER): KeycloakUiLogin | undefined {
    if (!keycloakBaseUrl) return undefined;
    return new KeycloakUiLogin(keycloakBaseUrl, user);
  }

  getUser(): TestUser {
    return this.user;
  }

  /**
   * Waits until the Keycloak login form, the app-ready selector or the "Sign In" link shows, and logs in if needed.
   * An app with an anonymous storefront shows "Sign In" instead of redirecting, so it is clicked to reach the login.
   */
  async ensureLoggedIn(page: Page, appReadySelector: string): Promise<void> {
    await page.waitForSelector(`${SUBMIT_SELECTOR}, ${appReadySelector}, ${SIGN_IN_SELECTOR}`, { timeout: TIMEOUT_MS });
    if (!this.isLoginPage(page) && !(await page.isVisible(appReadySelector)) && (await page.isVisible(SIGN_IN_SELECTOR))) {
      await page.click(SIGN_IN_SELECTOR);
      // Keycloak shows its form, or sends the browser straight back when it already has a session.
      await page.waitForSelector(`${SUBMIT_SELECTOR}, ${appReadySelector}`, { timeout: TIMEOUT_MS });
    }
    if (this.isLoginPage(page)) {
      await this.login(page);
      await page.waitForSelector(appReadySelector, { timeout: TIMEOUT_MS });
    }
  }

  /** Waits for the login form, without filling it in. */
  async waitForLoginForm(page: Page): Promise<void> {
    await page.waitForSelector(SUBMIT_SELECTOR, { timeout: TIMEOUT_MS });
  }

  isLoginPage(page: Page): boolean {
    return page.url().startsWith(this.keycloakBaseUrl);
  }

  private async login(page: Page): Promise<void> {
    await page.waitForSelector(SUBMIT_SELECTOR, { timeout: TIMEOUT_MS });
    await page.fill(USERNAME_SELECTOR, this.user.username);
    await page.fill(PASSWORD_SELECTOR, this.user.password);
    await page.click(SUBMIT_SELECTOR);
    await page.waitForURL((url) => !url.toString().startsWith(this.keycloakBaseUrl), { timeout: TIMEOUT_MS });
  }
}

const HOME_READY_SELECTOR = "a[href='/order-history']";

/** For raw Playwright tests: logs in when the app redirects to Keycloak; does nothing when auth is not configured. */
export async function loginToMyShopUiIfRequired(page: Page, keycloakBaseUrl: string | undefined): Promise<void> {
  await KeycloakUiLogin.forBaseUrl(keycloakBaseUrl)?.ensureLoggedIn(page, HOME_READY_SELECTOR);
}
