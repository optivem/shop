import type { ErpDriver } from '../../../../../../driver/port/external/erp/erp-driver.js';
import type { GetProductsResponse } from '../../../../../../driver/port/external/erp/dtos/GetProductsResponse.js';
import { success, failure, type Result } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../../driver/port/dtos/errors/SystemError.js';
import { UseCaseResult } from '../../../../shared/use-case-result.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';
import { BaseErpUseCase } from './base/BaseErpUseCase.js';
import { GetProductsVerification } from './GetProductsVerification.js';

export class GetProducts extends BaseErpUseCase<GetProductsResponse, GetProductsVerification> {
  constructor(driver: ErpDriver, context: UseCaseContext) {
    super(driver, context);
  }

  async execute(): Promise<UseCaseResult<GetProductsResponse, GetProductsVerification>> {
    const driverResult = await this.driver.getProducts();
    const result: Result<GetProductsResponse, SystemError> = driverResult.success
      ? success(driverResult.value)
      : failure({ message: driverResult.error.message ?? 'ERP error', fieldErrors: [] });

    return new UseCaseResult(
      result,
      this.context,
      (response, ctx) => new GetProductsVerification(response, ctx),
    );
  }
}
