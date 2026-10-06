import type { Page } from '@playwright/test';
import { TestUsers, type TestUser } from '../http/test-user.js';

const USERNAME_SELECTOR = '#username';
const PASSWORD_SELECTOR = '#password';
const SUBMIT_SELECTOR = '#kc-login';
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

  /** Waits until either the Keycloak login form or the app-ready selector shows, logging in if needed. */
  async ensureLoggedIn(page: Page, appReadySelector: string): Promise<void> {
    await page.waitForSelector(`${SUBMIT_SELECTOR}, ${appReadySelector}`, { timeout: TIMEOUT_MS });
    if (this.isLoginPage(page)) {
      await this.login(page);
      await page.waitForSelector(appReadySelector, { timeout: TIMEOUT_MS });
    }
  }

  private isLoginPage(page: Page): boolean {
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
