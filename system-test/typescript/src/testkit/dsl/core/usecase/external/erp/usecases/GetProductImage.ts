import type { ErpDriver } from '../../../../../../driver/port/external/erp/erp-driver.js';
import type { GetProductImageRequest } from '../../../../../../driver/port/external/erp/dtos/GetProductImageRequest.js';
import type { GetProductImageResponse } from '../../../../../../driver/port/external/erp/dtos/GetProductImageResponse.js';
import { success, failure, type Result } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../../driver/port/dtos/errors/SystemError.js';
import { UseCaseResult } from '../../../../shared/use-case-result.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';
import { BaseErpUseCase } from './base/BaseErpUseCase.js';
import { GetProductImageVerification } from './GetProductImageVerification.js';

export class GetProductImage extends BaseErpUseCase<GetProductImageResponse, GetProductImageVerification> {
  private _imageUrl?: string;

  constructor(driver: ErpDriver, context: UseCaseContext) {
    super(driver, context);
  }

  imageUrl(imageUrl: string): this {
    this._imageUrl = imageUrl;
    return this;
  }

  async execute(): Promise<UseCaseResult<GetProductImageResponse, GetProductImageVerification>> {
    const request: GetProductImageRequest = {
      imageUrl: this._imageUrl ?? '',
    };

    const driverResult = await this.driver.getProductImage(request);
    const result: Result<GetProductImageResponse, SystemError> = driverResult.success
      ? success(driverResult.value)
      : failure({ message: driverResult.error.message ?? 'ERP error', fieldErrors: [] });

    return new UseCaseResult(
      result,
      this.context,
      (response, ctx) => new GetProductImageVerification(response, ctx),
    );
  }
}
