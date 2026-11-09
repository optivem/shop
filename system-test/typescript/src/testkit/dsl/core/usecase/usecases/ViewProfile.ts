import type { MyShopDriver } from '../../../../driver/port/my-shop-driver.js';
import type { ViewProfileResponse } from '../../../../driver/port/dtos/ViewProfileResponse.js';
import { UseCaseResult } from '../../shared/use-case-result.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import { BaseMyShopUseCase } from './base/BaseMyShopUseCase.js';
import { ViewProfileVerification } from './ViewProfileVerification.js';

export class ViewProfile extends BaseMyShopUseCase<ViewProfileResponse, ViewProfileVerification> {
  constructor(driver: MyShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  async execute(): Promise<UseCaseResult<ViewProfileResponse, ViewProfileVerification>> {
    const result = await this.driver.viewProfile({});

    return new UseCaseResult(
      result,
      this.context,
      (response, ctx) => new ViewProfileVerification(response, ctx),
    );
  }
}
