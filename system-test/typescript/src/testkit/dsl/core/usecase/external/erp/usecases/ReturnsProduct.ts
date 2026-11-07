import type { ErpDriver } from '../../../../../../driver/port/external/erp/erp-driver.js';
import type { ReturnsProductRequest } from '../../../../../../driver/port/external/erp/dtos/ReturnsProductRequest.js';
import { success, failure, type Result } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../../driver/port/dtos/errors/SystemError.js';
import { UseCaseResult } from '../../../../shared/use-case-result.js';
import { VoidVerification } from '../../../../shared/void-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';
import { BaseErpUseCase } from './base/BaseErpUseCase.js';

export class ReturnsProduct extends BaseErpUseCase<void, VoidVerification> {
  private _skuAlias?: string;
  private _unitPrice: number | string = '';
  private _title?: string;
  private _description?: string;
  private _category?: string;
  private _brand?: string;

  constructor(driver: ErpDriver, context: UseCaseContext) {
    super(driver, context);
  }

  sku(skuAlias: string): this {
    this._skuAlias = skuAlias;
    return this;
  }

  unitPrice(unitPrice: number | string): this {
    this._unitPrice = unitPrice;
    return this;
  }

  title(title: string): this {
    this._title = title;
    return this;
  }

  description(description: string): this {
    this._description = description;
    return this;
  }

  category(category: string): this {
    this._category = category;
    return this;
  }

  brand(brand: string): this {
    this._brand = brand;
    return this;
  }

  async execute(): Promise<UseCaseResult<void, VoidVerification>> {
    const sku = this.context.getParamValue(this._skuAlias);
    const request: ReturnsProductRequest = {
      sku: sku ?? '',
      price: String(this._unitPrice),
      title: this._title,
      description: this._description,
      category: this._category,
      brand: this._brand,
    };
    const driverResult = await this.driver.returnsProduct(request);
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
