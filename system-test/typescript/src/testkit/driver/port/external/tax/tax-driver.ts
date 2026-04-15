import { Result } from '../../../../common/result.js';
import {
  ErrorResponse,
  GetTaxResponse,
  ReturnsTaxRateRequest,
} from '../../../../common/dtos.js';

export interface TaxDriver {
  goToTax(): Promise<Result<void, ErrorResponse>>;
  getTaxRate(country: string): Promise<Result<GetTaxResponse, ErrorResponse>>;
  returnsTaxRate(request: ReturnsTaxRateRequest): Promise<Result<void, ErrorResponse>>;
  close(): Promise<void>;
}
