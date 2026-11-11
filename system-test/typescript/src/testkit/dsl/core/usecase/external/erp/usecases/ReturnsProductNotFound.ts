import type { ErpDriver } from '../../../../../../driver/port/external/erp/erp-driver.js';
import { success, failure, type Result } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../../driver/port/dtos/errors/SystemError.js';
import { UseCaseResult } from '../../../../shared/use-case-result.js';
import { VoidVerification } from '../../../../shared/void-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';
import { BaseErpUseCase } from './base/BaseErpUseCase.js';

/** The ERP no longer offers a product: asking for it by SKU (an alias) answers "not found". */
export class ReturnsProductNotFound extends BaseErpUseCase<void, VoidVerification> {
  private _skuAlias: string | null = null;

  constructor(driver: ErpDriver, context: UseCaseContext) {
    super(driver, context);
  }

  sku(skuAlias: string): this {
    this._skuAlias = skuAlias;
    return this;
  }

  async execute(): Promise<UseCaseResult<void, VoidVerification>> {
    if (this._skuAlias === null) throw new Error('ReturnsProductNotFound needs a SKU');
    const driverResult = await this.driver.returnsProductNotFound({ sku: this.context.getParamValue(this._skuAlias) });
    const result: Result<void, SystemError> = driverResult.success
      ? success(undefined)
      : failure({ message: driverResult.error.message ?? 'ERP error', fieldErrors: [] });

    return new UseCaseResult(
      result,
      this.context,
      (_, ctx) => new VoidVerification(undefined, ctx),
    );
  }
}
