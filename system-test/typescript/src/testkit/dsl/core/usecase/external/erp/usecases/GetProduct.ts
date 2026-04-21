import type { ErpDriver } from '../../../../../../driver/port/external/erp/erp-driver.js';
import type { GetProductRequest } from '../../../../../../driver/port/external/erp/dtos/GetProductRequest.js';
import type { GetProductResponse } from '../../../../../../driver/port/external/erp/dtos/GetProductResponse.js';
import { success, failure, type Result } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../../driver/port/shop/dtos/errors/SystemError.js';
import { UseCaseResult } from '../../../../shared/use-case-result.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';
import { BaseErpUseCase } from './base/BaseErpUseCase.js';
import { GetProductVerification } from './GetProductVerification.js';

export class GetProduct extends BaseErpUseCase<GetProductResponse, GetProductVerification> {
  private _skuParamAlias?: string;

  constructor(driver: ErpDriver, context: UseCaseContext) {
    super(driver, context);
  }

  sku(skuParamAlias: string): this {
    this._skuParamAlias = skuParamAlias;
    return this;
  }

  async execute(): Promise<UseCaseResult<GetProductResponse, GetProductVerification>> {
    const sku = this.context.getParamValue(this._skuParamAlias);

    const request: GetProductRequest = {
      sku: sku ?? '',
    };

    const driverResult = await this.driver.getProduct(request);
    const result: Result<GetProductResponse, SystemError> = driverResult.success
      ? success(driverResult.value)
      : failure({ message: driverResult.error.message ?? 'ERP error', fieldErrors: [] });

    return new UseCaseResult(
      result,
      this.context,
      (response, ctx) => new GetProductVerification(response, ctx),
    );
  }
}
