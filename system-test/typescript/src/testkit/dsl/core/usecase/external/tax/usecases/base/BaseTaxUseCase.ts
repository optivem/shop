import type { TaxDriver } from '../../../../../../../driver/port/external/tax/tax-driver.js';
import { BaseUseCase } from '../../../../../shared/base-use-case.js';

export abstract class BaseTaxUseCase<TResponse, TVerification> extends BaseUseCase<
  TaxDriver,
  TResponse,
  TVerification
> {}
