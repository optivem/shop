import type { MyShopDriver } from '../../../../driver/port/my-shop-driver.js';
import type { BrowseCustomersResponse } from '../../../../driver/port/dtos/BrowseCustomersResponse.js';
import { UseCaseResult } from '../../shared/use-case-result.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import { BaseMyShopUseCase } from './base/BaseMyShopUseCase.js';
import { BrowseCustomersVerification } from './BrowseCustomersVerification.js';

export class BrowseCustomers extends BaseMyShopUseCase<BrowseCustomersResponse, BrowseCustomersVerification> {
  constructor(driver: MyShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  async execute(): Promise<UseCaseResult<BrowseCustomersResponse, BrowseCustomersVerification>> {
    const result = await this.driver.browseCustomers({});

    return new UseCaseResult(
      result,
      this.context,
      (response, ctx) => new BrowseCustomersVerification(response, ctx),
    );
  }
}
