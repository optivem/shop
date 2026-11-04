import type { MyShopDriver } from '../../../../driver/port/my-shop-driver.js';
import type { PlaceOrderRequest } from '../../../../driver/port/dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from '../../../../driver/port/dtos/PlaceOrderResponse.js';
import { UseCaseResult } from '../../shared/use-case-result.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import { BaseMyShopUseCase } from './base/BaseMyShopUseCase.js';
import { PlaceOrderVerification } from './PlaceOrderVerification.js';

export class PlaceOrder extends BaseMyShopUseCase<PlaceOrderResponse, PlaceOrderVerification> {
  private _skuAlias?: string;
  private _quantity: number | string = '';
  private _countryAlias?: string;
  private _couponCodeAlias?: string;
  private _orderNumberResultAlias?: string;
  private _idempotencyKeyAlias?: string;

  constructor(driver: MyShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  sku(skuAlias: string): this {
    this._skuAlias = skuAlias;
    return this;
  }

  quantity(quantity: number | string): this {
    this._quantity = quantity;
    return this;
  }

  country(countryAlias: string): this {
    this._countryAlias = countryAlias;
    return this;
  }

  couponCode(couponCodeAlias: string): this {
    this._couponCodeAlias = couponCodeAlias;
    return this;
  }

  orderNumber(orderNumberResultAlias: string): this {
    this._orderNumberResultAlias = orderNumberResultAlias;
    return this;
  }

  idempotencyKey(idempotencyKeyAlias: string): this {
    this._idempotencyKeyAlias = idempotencyKeyAlias;
    return this;
  }

  async execute(): Promise<UseCaseResult<PlaceOrderResponse, PlaceOrderVerification>> {
    const sku = this.context.getParamValue(this._skuAlias);
    const country = this.context.getParamValueOrLiteral(this._countryAlias);
    const couponCode = this.context.getParamValue(this._couponCodeAlias);

    const request: PlaceOrderRequest = {
      sku: sku ?? '',
      quantity: String(this._quantity),
      country: country ?? '',
      couponCode: couponCode ?? undefined,
      // Keys are the shop's own data, so the alias always maps to a scenario-unique value.
      idempotencyKey: this.context.getParamValue(this._idempotencyKeyAlias),
    };
    const result = await this.driver.placeOrder(request);

    if (this._orderNumberResultAlias && result.success) {
      this.context.setResultEntry(this._orderNumberResultAlias, result.value.orderNumber);
    }

    return new UseCaseResult(
      result,
      this.context,
      (response, ctx) => new PlaceOrderVerification(response, ctx),
    );
  }
}
