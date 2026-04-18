import type { ErpDriver } from '../../../../../../driver/port/external/erp/erp-driver.js';
import { success, failure, type Result } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../../driver/port/shop/dtos/errors/SystemError.js';
import { UseCaseResult } from '../../../../shared/use-case-result.js';
import { VoidVerification } from '../../../../shared/void-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';
import { BaseErpUseCase } from './base/BaseErpUseCase.js';

export class GoToErp extends BaseErpUseCase<void, VoidVerification> {
  constructor(driver: ErpDriver, context: UseCaseContext) {
    super(driver, context);
  }

  async execute(): Promise<UseCaseResult<void, VoidVerification>> {
    const driverResult = await this.driver.goToErp();
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
