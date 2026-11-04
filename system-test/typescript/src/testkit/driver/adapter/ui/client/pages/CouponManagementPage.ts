import { BasePage, PAGE_TIMEOUT_MS } from './BasePage.js';
import { nonEmptyOr } from '../../../../../common/fallback.js';
import { Converter } from '../../../../../common/converter.js';

const COUPON_COLUMN_COUNT = 6;

interface CouponRow {
  code: string;
  discountRate: string;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number;
  usedCount: number;
}

function parseDisplayedDateToIso(displayed: string | undefined): string | undefined {
  if (!displayed || displayed.trim() === '') return undefined;
  const parsed = new Date(displayed.trim());
  if (Number.isNaN(parsed.getTime())) return displayed.trim();
  const utcMs = Date.UTC(
    parsed.getFullYear(), parsed.getMonth(), parsed.getDate(),
    parsed.getHours(), parsed.getMinutes(), parsed.getSeconds(),
  );
  return new Date(utcMs).toISOString().replace('.000Z', 'Z');
}

export const COUPON_MANAGEMENT_READY_SELECTOR = '[aria-label="Coupon Code"]';

export class CouponManagementPage extends BasePage {
  async inputCouponCode(code: string): Promise<void> {
    await this.page.locator('[aria-label="Coupon Code"]').fill(code, { timeout: PAGE_TIMEOUT_MS });
  }

  async inputDiscountRate(rate: number | string): Promise<void> {
    await this.page.locator('[aria-label="Discount Rate"]').fill(String(rate), { timeout: PAGE_TIMEOUT_MS });
  }

  async inputValidFrom(dateStr: string): Promise<void> {
    const localValue = dateStr.replace('Z', '').replace('T', 'T').slice(0, 16);
    await this.page.locator('[aria-label="Valid From"]').fill(localValue, { timeout: PAGE_TIMEOUT_MS });
  }

  async inputValidTo(dateStr: string): Promise<void> {
    const localValue = dateStr.replace('Z', '').replace('T', 'T').slice(0, 16);
    await this.page.locator('[aria-label="Valid To"]').fill(localValue, { timeout: PAGE_TIMEOUT_MS });
  }

  async inputUsageLimit(limit: number | string): Promise<void> {
    await this.page.locator('[aria-label="Usage Limit"]').fill(String(limit), { timeout: PAGE_TIMEOUT_MS });
  }

  async clickPublishCoupon(): Promise<void> {
    await this.page.locator('[aria-label="Create Coupon"]').click({ timeout: PAGE_TIMEOUT_MS });
  }

  async clickRefreshCouponList(): Promise<void> {
    await this.page.locator('[aria-label="Refresh Coupon List"]').click({ timeout: PAGE_TIMEOUT_MS });
  }

  async getCouponRows(): Promise<CouponRow[]> {
    const table = this.page.locator("[aria-label='Coupons Table']");
    await table.waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
    // The list has finished loading once it shows either a coupon row or its empty-state row.
    await table
      .locator('tbody tr td:nth-child(2), tbody tr td:has-text("No coupons found")')
      .first()
      .waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
    const rows = table.locator('tbody tr');
    const count = await rows.count();
    const result: CouponRow[] = [];
    for (let i = 0; i < count; i++) {
      const cells = rows.nth(i).locator('td');
      // An empty list renders one colspan "No coupons found" row, which is not a coupon.
      if ((await cells.count()) < COUPON_COLUMN_COUNT) continue;
      const code = (await cells.nth(0).textContent())?.trim() ?? '';
      const rateText = nonEmptyOr((await cells.nth(1).textContent())?.trim(), '0');
      const discountRate = Converter.fromDisplayedPercent(rateText);
      const validFrom = parseDisplayedDateToIso((await cells.nth(2).textContent()) ?? undefined);
      const validTo = parseDisplayedDateToIso((await cells.nth(3).textContent()) ?? undefined);
      const usageLimitText = (await cells.nth(4).textContent())?.trim() ?? '';
      const usageLimit = usageLimitText === 'Unlimited' || usageLimitText === '' ? undefined : Number.parseInt(usageLimitText, 10);
      const usedCountText = (await cells.nth(5).textContent())?.trim() ?? '0';
      const usedCount = Number.parseInt(usedCountText, 10) || 0;
      result.push({ code, discountRate, validFrom, validTo, usageLimit, usedCount });
    }
    return result;
  }
}
