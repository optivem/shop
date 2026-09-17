import type { MyShopDriver } from '../../../../driver/port/my-shop-driver.js';
import type { ViewOrderResponse } from '../../../../driver/port/dtos/ViewOrderResponse.js';
import { UseCaseResult } from '../../shared/use-case-result.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import { BaseMyShopUseCase } from './base/BaseMyShopUseCase.js';
import { ViewOrderVerification } from './ViewOrderVerification.js';

export class ViewOrder extends BaseMyShopUseCase<ViewOrderResponse, ViewOrderVerification> {
  private _orderNumber = '';

  constructor(driver: MyShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  orderNumber(aliasOrValue: string): this {
    this._orderNumber = aliasOrValue;
    return this;
  }

  async execute(): Promise<UseCaseResult<ViewOrderResponse, ViewOrderVerification>> {
    const resolved = this.context.getResultValue(this._orderNumber) ?? this._orderNumber;
    const result = await this.driver.viewOrder({ orderNumber: resolved });

    return new UseCaseResult(
      result,
      this.context,
      (response, ctx) => new ViewOrderVerification(response, ctx),
    );
  }
}
