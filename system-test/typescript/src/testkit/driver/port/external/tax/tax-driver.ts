import { Result } from '../../../../common/result.js';
import { TaxErrorResponse } from './dtos/TaxErrorResponse.js';
import { GetCountryRequest } from './dtos/GetCountryRequest.js';
import { GetTaxResponse } from './dtos/GetTaxResponse.js';
import { ReturnsTaxRateRequest } from './dtos/ReturnsTaxRateRequest.js';

export interface TaxDriver {
  goToTax(): Promise<Result<void, TaxErrorResponse>>;
  getTaxRate(request: GetCountryRequest): Promise<Result<GetTaxResponse, TaxErrorResponse>>;
  returnsTaxRate(request: ReturnsTaxRateRequest): Promise<Result<void, TaxErrorResponse>>;
  close(): Promise<void>;
}
