import type { Result } from '../../../../common/result.js';
import type { TaxErrorResponse } from './dtos/errors/TaxErrorResponse.js';
import type { GetCountryRequest } from './dtos/GetCountryRequest.js';
import type { GetTaxResponse } from './dtos/GetTaxResponse.js';
import type { ReturnsTaxRateRequest } from './dtos/ReturnsTaxRateRequest.js';

export interface TaxDriver {
  goToTax(): Promise<Result<void, TaxErrorResponse>>;
  getTaxRate(request: GetCountryRequest): Promise<Result<GetTaxResponse, TaxErrorResponse>>;
  returnsTaxRate(request: ReturnsTaxRateRequest): Promise<Result<void, TaxErrorResponse>>;
  close(): Promise<void>;
}
