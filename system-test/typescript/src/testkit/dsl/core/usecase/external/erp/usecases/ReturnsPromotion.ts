import type { ErpDriver } from '../../../../../../driver/port/external/erp/erp-driver.js';
import type { ReturnsPromotionRequest } from '../../../../../../driver/port/external/erp/dtos/ReturnsPromotionRequest.js';
import { success, failure, type Result } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../../driver/port/shop/dtos/errors/SystemError.js';
import { UseCaseResult } from '../../../../shared/use-case-result.js';
import { VoidVerification } from '../../../../shared/void-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';
import { BaseErpUseCase } from './base/BaseErpUseCase.js';

export class ReturnsPromotion extends BaseErpUseCase<void, VoidVerification> {
  private _promotionActive: boolean = false;
  private _discount: number | string = '';

  constructor(driver: ErpDriver, context: UseCaseContext) {
    super(driver, context);
  }

  withActive(promotionActive: boolean): this {
    this._promotionActive = promotionActive;
    return this;
  }

  withDiscount(discount: number | string): this {
    this._discount = discount;
    return this;
  }

  async execute(): Promise<UseCaseResult<void, VoidVerification>> {
    const request: ReturnsPromotionRequest = {
      promotionActive: this._promotionActive,
      discount: String(this._discount),
    };

    const driverResult = await this.driver.returnsPromotion(request);
    const result: Result<void, SystemError> = driverResult.success
      ? success(undefined)
      : failure({ message: driverResult.error.message ?? 'ERP error', fieldErrors: [] });

    return new UseCaseResult(
      result,
      this.context,
      (_, ctx) => new VoidVerification(undefined, ctx),
    );
  }
}
