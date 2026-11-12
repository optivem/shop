import type { MyShopDriver } from '../../../../driver/port/my-shop-driver.js';
import type { ViewProfileResponse } from '../../../../driver/port/dtos/ViewProfileResponse.js';
import { SIGN_IN } from '../../../../driver/port/dtos/ViewProfileRequest.js';
import { UseCaseResult } from '../../shared/use-case-result.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import { BaseMyShopUseCase } from './base/BaseMyShopUseCase.js';
import { ViewProfileVerification } from './ViewProfileVerification.js';

export class ViewProfile extends BaseMyShopUseCase<ViewProfileResponse, ViewProfileVerification> {
  private whenAskedToSignIn: string | null = null;

  constructor(driver: MyShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  /** A visitor asked to sign in signs in as the customer (UI only). */
  signInWhenAsked(): this {
    this.whenAskedToSignIn = SIGN_IN;
    return this;
  }

  async execute(): Promise<UseCaseResult<ViewProfileResponse, ViewProfileVerification>> {
    const result = await this.driver.viewProfile({ whenAskedToSignIn: this.whenAskedToSignIn });

    return new UseCaseResult(
      result,
      this.context,
      (response, ctx) => new ViewProfileVerification(response, ctx),
    );
  }
}
