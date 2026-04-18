import type { TaxDriver } from '../../../../../../driver/port/external/tax/tax-driver.js';
import type { GetCountryRequest } from '../../../../../../driver/port/external/tax/dtos/GetCountryRequest.js';
import type { GetTaxResponse } from '../../../../../../driver/port/external/tax/dtos/GetTaxResponse.js';
import { success, failure, type Result } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../../driver/port/shop/dtos/errors/SystemError.js';
import { UseCaseResult } from '../../../../shared/use-case-result.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';
import { BaseTaxUseCase } from './base/BaseTaxUseCase.js';
import { GetTaxVerification } from './GetTaxVerification.js';

export class GetTaxRate extends BaseTaxUseCase<GetTaxResponse, GetTaxVerification> {
  private _countryValueOrAlias?: string;

  constructor(driver: TaxDriver, context: UseCaseContext) {
    super(driver, context);
  }

  country(countryValueOrAlias: string): this {
    this._countryValueOrAlias = countryValueOrAlias;
    return this;
  }

  async execute(): Promise<UseCaseResult<GetTaxResponse, GetTaxVerification>> {
    const country = this.context.getParamValueOrLiteral(this._countryValueOrAlias);

    const request: GetCountryRequest = {
      country: country ?? '',
    };

    const driverResult = await this.driver.getTaxRate(request);
    const result: Result<GetTaxResponse, SystemError> = driverResult.success
      ? success(driverResult.value)
      : failure({ message: driverResult.error.message ?? 'Tax error', fieldErrors: [] });

    return new UseCaseResult(
      result,
      this.context,
      (response, ctx) => new GetTaxVerification(response, ctx),
    );
  }
}
