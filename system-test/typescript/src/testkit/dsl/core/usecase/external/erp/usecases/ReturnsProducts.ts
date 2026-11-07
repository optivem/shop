import type { ErpDriver } from '../../../../../../driver/port/external/erp/erp-driver.js';
import type { ReturnsProductsRequest } from '../../../../../../driver/port/external/erp/dtos/ReturnsProductsRequest.js';
import { success, failure, type Result } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../../driver/port/dtos/errors/SystemError.js';
import { UseCaseResult } from '../../../../shared/use-case-result.js';
import { VoidVerification } from '../../../../shared/void-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';
import { BaseErpUseCase } from './base/BaseErpUseCase.js';

interface ProductEntry {
  skuAlias: string;
  unitPrice: number | string;
}

/** Stages the ERP's enumerable product list (GET /api/products); an empty list stages no products. */
export class ReturnsProducts extends BaseErpUseCase<void, VoidVerification> {
  private readonly _products: ProductEntry[] = [];

  constructor(driver: ErpDriver, context: UseCaseContext) {
    super(driver, context);
  }

  product(skuAlias: string, unitPrice: number | string): this {
    this._products.push({ skuAlias, unitPrice });
    return this;
  }

  async execute(): Promise<UseCaseResult<void, VoidVerification>> {
    const request: ReturnsProductsRequest = {
      products: this._products.map((product) => ({
        sku: this.context.getParamValue(product.skuAlias) ?? '',
        price: String(product.unitPrice),
      })),
    };
    const driverResult = await this.driver.returnsProducts(request);
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
