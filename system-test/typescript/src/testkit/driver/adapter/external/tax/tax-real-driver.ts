import type { Result } from '../../../../common/result.js';
import { success } from '../../../../common/result.js';
import type { TaxErrorResponse } from '../../../port/external/tax/dtos/errors/TaxErrorResponse.js';
import type { ReturnsTaxRateRequest } from '../../../port/external/tax/dtos/ReturnsTaxRateRequest.js';
import { BaseTaxDriver } from './BaseTaxDriver.js';
import { TaxRealClient } from './client/TaxRealClient.js';

export class TaxRealDriver extends BaseTaxDriver<TaxRealClient> {
  constructor(baseUrl: string) {
    super(new TaxRealClient(baseUrl));
  }

  returnsTaxRate(_request: ReturnsTaxRateRequest): Promise<Result<void, TaxErrorResponse>> {
    return Promise.resolve(success(undefined));
  }

  close(): Promise<void> {
    return Promise.resolve();
  }
}
