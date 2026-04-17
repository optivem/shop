import type { ErpDriver } from '../../../../../../../driver/port/external/erp/erp-driver.js';
import { BaseUseCase } from '../../../../../shared/base-use-case.js';

export abstract class BaseErpUseCase<TResponse, TVerification> extends BaseUseCase<
  ErpDriver,
  TResponse,
  TVerification
> {}
