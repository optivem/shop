import type { ShopDriver } from '../../../../../driver/port/shop/shop-driver.js';
import type { BrowseCouponsResponse } from '../../../../../driver/port/shop/dtos/BrowseCouponsResponse.js';
import { UseCaseResult } from '../../../shared/use-case-result.js';
import type { UseCaseContext } from '../../../shared/use-case-context.js';
import { BaseShopUseCase } from './base/BaseShopUseCase.js';
import { BrowseCouponsVerification } from './BrowseCouponsVerification.js';

export class BrowseCoupons extends BaseShopUseCase<BrowseCouponsResponse, BrowseCouponsVerification> {
  constructor(driver: ShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  async execute(): Promise<UseCaseResult<BrowseCouponsResponse, BrowseCouponsVerification>> {
    const result = await this.driver.browseCoupons();

    return new UseCaseResult(
      result,
      this.context,
      (response, ctx) => new BrowseCouponsVerification(response, ctx),
    );
  }
}
