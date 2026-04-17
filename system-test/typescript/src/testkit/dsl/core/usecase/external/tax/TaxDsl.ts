import type { Result } from '../../../../../common/result.js';
import type { TaxDriver } from '../../../../../driver/port/external/tax/tax-driver.js';
import type { TaxErrorResponse } from '../../../../../driver/port/external/tax/dtos/TaxErrorResponse.js';
import type { UseCaseContext } from '../../../use-case-context.js';

export class TaxDsl {
  constructor(
    private readonly driver: TaxDriver,
    private readonly context: UseCaseContext,
  ) {}

  async goToTax(): Promise<Result<void, TaxErrorResponse>> {
    return this.driver.goToTax();
  }

  async close(): Promise<void> {
    await this.driver.close();
  }
}
