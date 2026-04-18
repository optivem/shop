import type { ShopDriver } from '../../../../../driver/port/shop/shop-driver.js';
import { UseCaseResult } from '../../../shared/use-case-result.js';
import { VoidVerification } from '../../../shared/void-verification.js';
import type { UseCaseContext } from '../../../shared/use-case-context.js';
import { BaseShopUseCase } from './base/BaseShopUseCase.js';

export class CancelOrder extends BaseShopUseCase<void, VoidVerification> {
  private _orderNumberResultAlias: string = '';

  constructor(driver: ShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  orderNumber(orderNumberResultAlias: string): this {
    this._orderNumberResultAlias = orderNumberResultAlias;
    return this;
  }

  async execute(): Promise<UseCaseResult<void, VoidVerification>> {
    const orderNumber = this.context.getResultValue(this._orderNumberResultAlias) ?? this._orderNumberResultAlias;
    const result = await this.driver.cancelOrder(orderNumber);

    return new UseCaseResult(
      result,
      this.context,
      (_, ctx) => new VoidVerification(undefined, ctx),
    );
  }
}
