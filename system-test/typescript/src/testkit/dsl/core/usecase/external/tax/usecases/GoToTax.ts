import type { TaxDriver } from '../../../../../../driver/port/external/tax/tax-driver.js';
import { success, failure, type Result } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../../driver/port/shop/dtos/errors/SystemError.js';
import { UseCaseResult } from '../../../../shared/use-case-result.js';
import { VoidVerification } from '../../../../shared/void-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';
import { BaseTaxUseCase } from './base/BaseTaxUseCase.js';

export class GoToTax extends BaseTaxUseCase<void, VoidVerification> {
  constructor(driver: TaxDriver, context: UseCaseContext) {
    super(driver, context);
  }

  async execute(): Promise<UseCaseResult<void, VoidVerification>> {
    const driverResult = await this.driver.goToTax();
    const result: Result<void, SystemError> = driverResult.success
      ? success(undefined)
      : failure({ message: driverResult.error.message ?? 'Tax error', fieldErrors: [] });

    return new UseCaseResult(
      result,
      this.context,
      (_, ctx) => new VoidVerification(undefined, ctx),
    );
  }
}
