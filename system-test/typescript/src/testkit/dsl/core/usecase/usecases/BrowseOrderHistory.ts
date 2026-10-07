import type { MyShopDriver } from '../../../../driver/port/my-shop-driver.js';
import type { BrowseOrderHistoryResponse } from '../../../../driver/port/dtos/BrowseOrderHistoryResponse.js';
import { UseCaseResult } from '../../shared/use-case-result.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import { BaseMyShopUseCase } from './base/BaseMyShopUseCase.js';
import { BrowseOrderHistoryVerification } from './BrowseOrderHistoryVerification.js';

export class BrowseOrderHistory extends BaseMyShopUseCase<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification> {
  private _orderNumber = '';

  constructor(driver: MyShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  orderNumber(aliasOrValue: string): this {
    this._orderNumber = aliasOrValue;
    return this;
  }

  async execute(): Promise<UseCaseResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>> {
    const resolved = this.context.getResultValue(this._orderNumber) ?? this._orderNumber;
    const result = await this.driver.browseOrderHistory({ orderNumber: resolved });

    return new UseCaseResult(
      result,
      this.context,
      (response, ctx) => new BrowseOrderHistoryVerification(response, ctx),
    );
  }
}
