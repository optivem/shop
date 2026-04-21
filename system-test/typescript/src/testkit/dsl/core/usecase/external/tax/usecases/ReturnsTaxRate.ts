import type { TaxDriver } from '../../../../../../driver/port/external/tax/tax-driver.js';
import type { ReturnsTaxRateRequest } from '../../../../../../driver/port/external/tax/dtos/ReturnsTaxRateRequest.js';
import { success, failure, type Result } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../../driver/port/shop/dtos/errors/SystemError.js';
import { UseCaseResult } from '../../../../shared/use-case-result.js';
import { VoidVerification } from '../../../../shared/void-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';
import { BaseTaxUseCase } from './base/BaseTaxUseCase.js';

export class ReturnsTaxRate extends BaseTaxUseCase<void, VoidVerification> {
  private _countryAlias?: string;
  private _taxRate: number | string = '';

  constructor(driver: TaxDriver, context: UseCaseContext) {
    super(driver, context);
  }

  country(countryAlias: string): this {
    this._countryAlias = countryAlias;
    return this;
  }

  taxRate(taxRate: number | string): this {
    this._taxRate = taxRate;
    return this;
  }

  async execute(): Promise<UseCaseResult<void, VoidVerification>> {
    const country = this.context.getParamValueOrLiteral(this._countryAlias);

    const request: ReturnsTaxRateRequest = {
      country: country ?? '',
      taxRate: String(this._taxRate),
    };

    const driverResult = await this.driver.returnsTaxRate(request);
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
