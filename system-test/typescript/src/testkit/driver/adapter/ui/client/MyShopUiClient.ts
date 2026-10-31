import type { Browser, BrowserContext, Page } from '@playwright/test';
import type { Result } from '../../../../common/result.js';
import { success, failure } from '../../../../common/result.js';
import type { SystemError } from '../../../port/dtos/errors/SystemError.js';
import { TestUsers, type TestUser } from '../../shared/client/http/test-user.js';
import { KeycloakUiLogin } from '../../shared/client/playwright/keycloak-ui-login.js';
import { PAGE_TIMEOUT_MS } from './pages/BasePage.js';
import { HomePage } from './pages/HomePage.js';
import { NEW_ORDER_READY_SELECTOR, NewOrderPage } from './pages/NewOrderPage.js';
import { OrderHistoryPage } from './pages/OrderHistoryPage.js';
import { OrderDetailsPage } from './pages/OrderDetailsPage.js';
import { COUPON_MANAGEMENT_READY_SELECTOR, CouponManagementPage } from './pages/CouponManagementPage.js';

const HOME_READY_SELECTOR = "a[href='/order-history']";
const SINGLE_PAGE_APP_ROOT_SELECTOR = '#root';
const NOT_AUTHORIZED_SELECTOR = "h1:has-text('Not authorized'):visible";
const NEW_ORDER_PATH = '/new-order';
const COUPON_MANAGEMENT_PATH = '/admin-coupons';
const FORBIDDEN_STATUS = 403;

export const AUTHENTICATION_REQUIRED = 'Authentication is required';
export const PERMISSION_DENIED = 'You do not have permission to perform this action';

export class MyShopUiClient {
  private context: BrowserContext | null = null;
  private currentPage: Page | null = null;
  private login: KeycloakUiLogin | undefined;
  private lastResponseStatus: number | undefined;

  /**
   * @param keycloakBaseUrl when undefined or empty, the app is assumed to have no login screen.
   */
  constructor(
    private readonly baseUrl: string,
    private readonly browser: Browser,
    private readonly keycloakBaseUrl?: string,
  ) {
    this.login = KeycloakUiLogin.forBaseUrl(keycloakBaseUrl, TestUsers.CUSTOMER);
  }

  /**
   * Makes `user` the logged-in user. Switching discards the browser session (the next openHomePage()
   * creates a fresh isolated context and logs in as the new user). No-op without Keycloak or when already that user.
   */
  async switchUser(user: TestUser): Promise<void> {
    if (!this.login || this.login.getUser() === user) return;
    await this.close();
    this.login = KeycloakUiLogin.forBaseUrl(this.keycloakBaseUrl, user);
  }

  async openHomePage(): Promise<Result<HomePage, SystemError>> {
    try {
      if (!this.context) {
        this.context = await this.browser.newContext({ viewport: { width: 1920, height: 1080 } });
        this.currentPage = await this.context.newPage();
      }
      const page = this.requirePage();
      const response = await page.goto(this.baseUrl);
      if (response?.status() === 200) {
        await this.login?.ensureLoggedIn(page, HOME_READY_SELECTOR);
        return success(new HomePage(page));
      }
      return failure({ message: `MyShop UI not available: ${response?.status()}`, fieldErrors: [] });
    } catch (e) {
      return failure({ message: `MyShop UI not available: ${String(e)}`, fieldErrors: [] });
    }
  }

  /** Opens an order by its URL, not through the order history. The app must already be open. */
  async openOrderDetailsPage(orderNumber: string): Promise<OrderDetailsPage> {
    const encodedOrderNumber = encodeURIComponent(orderNumber);
    await this.openDirectly(`/order-details?orderNumber=${encodedOrderNumber}`, `/order-details/${encodedOrderNumber}`);
    return new OrderDetailsPage(this.requirePage());
  }

  /** Opens the new order page by its URL, reporting a refusal. The app must already be open. */
  async openNewOrderPage(): Promise<Result<NewOrderPage, SystemError>> {
    return this.openRestrictedPage(NEW_ORDER_PATH, NEW_ORDER_READY_SELECTOR, new NewOrderPage(this.requirePage()));
  }

  /** Opens the coupon management page by its URL, reporting a refusal. The app must already be open. */
  async openCouponManagementPage(): Promise<Result<CouponManagementPage, SystemError>> {
    return this.openRestrictedPage(
      COUPON_MANAGEMENT_PATH,
      COUPON_MANAGEMENT_READY_SELECTOR,
      new CouponManagementPage(this.requirePage()),
    );
  }

  /** Opens `path` in a throwaway session that never logs in, and requires the app to send it to the login form. */
  async assertLoginRequiredFor(path: string): Promise<void> {
    if (!this.login) {
      throw new Error('Anonymous access can only be exercised against a system with a login');
    }
    const anonymousContext = await this.browser.newContext();
    try {
      const anonymousPage = await anonymousContext.newPage();
      await anonymousPage.goto(`${this.baseUrl}${path}`);
      await this.login.waitForLoginForm(anonymousPage);
    } finally {
      await anonymousContext.close();
    }
  }

  newOrderPage(): NewOrderPage {
    return new NewOrderPage(this.requirePage());
  }

  orderHistoryPage(): OrderHistoryPage {
    return new OrderHistoryPage(this.requirePage());
  }

  couponManagementPage(): CouponManagementPage {
    return new CouponManagementPage(this.requirePage());
  }

  async close(): Promise<void> {
    if (this.currentPage) {
      await this.currentPage.close();
      this.currentPage = null;
    }
    if (this.context) {
      await this.context.close();
      this.context = null;
    }
  }

  /** The server-rendered UIs refuse with a 403 response; the React frontend renders a "Not authorized" page. */
  private async openRestrictedPage<T>(
    path: string,
    readySelector: string,
    openedPage: T,
  ): Promise<Result<T, SystemError>> {
    await this.openDirectly(path, path);

    if (this.lastResponseStatus === FORBIDDEN_STATUS) {
      return failure({ message: PERMISSION_DENIED, fieldErrors: [] });
    }

    const page = this.requirePage();
    await page
      .locator(`${readySelector}, ${NOT_AUTHORIZED_SELECTOR}`)
      .waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });

    if ((await page.locator(NOT_AUTHORIZED_SELECTOR).count()) > 0) {
      return failure({ message: PERMISSION_DENIED, fieldErrors: [] });
    }
    return success(openedPage);
  }

  /**
   * Server-rendered UIs are simply navigated to. The React frontend keeps its session in memory and returns to
   * the home page after the login redirect, so a reload would lose the URL: it is moved to the route in place.
   */
  private async openDirectly(serverPath: string, singlePageAppRoute: string): Promise<void> {
    const page = this.requirePage();
    if ((await page.locator(SINGLE_PAGE_APP_ROOT_SELECTOR).count()) > 0) {
      const route = JSON.stringify(singlePageAppRoute);
      await page.evaluate(`history.pushState({}, '', ${route}); dispatchEvent(new PopStateEvent('popstate'));`);
    } else {
      const response = await page.goto(`${this.baseUrl}${serverPath}`);
      this.lastResponseStatus = response?.status();
    }
  }

  private requirePage(): Page {
    if (!this.currentPage) {
      throw new Error('MyShopUiClient: openHomePage() must be called before accessing pages');
    }
    return this.currentPage;
  }
}
