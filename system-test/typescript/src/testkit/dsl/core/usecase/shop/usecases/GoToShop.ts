import type { ShopDriver } from '../../../../../driver/port/shop/shop-driver.js';
import { UseCaseResult } from '../../../shared/use-case-result.js';
import { VoidVerification } from '../../../shared/void-verification.js';
import type { UseCaseContext } from '../../../shared/use-case-context.js';
import { BaseShopUseCase } from './base/BaseShopUseCase.js';

export class GoToShop extends BaseShopUseCase<void, VoidVerification> {
  constructor(driver: ShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  async execute(): Promise<UseCaseResult<void, VoidVerification>> {
    const result = await this.driver.goToShop();

    return new UseCaseResult(
      result,
      this.context,
      (_, ctx) => new VoidVerification(undefined, ctx),
    );
  }
}
