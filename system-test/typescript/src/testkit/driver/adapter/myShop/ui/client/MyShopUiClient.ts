import type { Browser, BrowserContext, Page } from 'playwright';
import type { Result } from '../../../../../common/result.js';
import { success, failure } from '../../../../../common/result.js';
import type { SystemError } from '../../../../port/myShop/dtos/errors/SystemError.js';
import { HomePage } from './pages/HomePage.js';
import { NewOrderPage } from './pages/NewOrderPage.js';
import { OrderHistoryPage } from './pages/OrderHistoryPage.js';
import { OrderDetailsPage } from './pages/OrderDetailsPage.js';
import { CouponManagementPage } from './pages/CouponManagementPage.js';

export class MyShopUiClient {
  private context: BrowserContext | null = null;
  private currentPage: Page | null = null;

  constructor(
    private readonly baseUrl: string,
    private readonly browser: Browser,
  ) {}

  async openHomePage(): Promise<Result<HomePage, SystemError>> {
    try {
      if (!this.context) {
        this.context = await this.browser.newContext({ viewport: { width: 1920, height: 1080 } });
        this.currentPage = await this.context.newPage();
      }
      const response = await this.currentPage!.goto(this.baseUrl);
      if (response?.status() === 200) {
        return success(new HomePage(this.currentPage!));
      }
      return failure({ message: `MyShop UI not available: ${response?.status()}`, fieldErrors: [] });
    } catch (e) {
      return failure({ message: `MyShop UI not available: ${e}`, fieldErrors: [] });
    }
  }

  newOrderPage(): NewOrderPage {
    return new NewOrderPage(this.requirePage());
  }

  orderHistoryPage(): OrderHistoryPage {
    return new OrderHistoryPage(this.requirePage());
  }

  orderDetailsPage(): OrderDetailsPage {
    return new OrderDetailsPage(this.requirePage());
  }

  couponManagementPage(): CouponManagementPage {
    return new CouponManagementPage(this.requirePage());
  }

  async close(): Promise<void> {
    if (this.currentPage) {
      await this.currentPage.close().catch(() => {});
      this.currentPage = null;
    }
    if (this.context) {
      await this.context.close().catch(() => {});
      this.context = null;
    }
  }

  private requirePage(): Page {
    if (!this.currentPage) {
      throw new Error('MyShopUiClient: openHomePage() must be called before accessing pages');
    }
    return this.currentPage;
  }
}
