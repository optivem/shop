import type { MyShopDriver } from '../../../../driver/port/my-shop-driver.js';
import {
  BACK_TO_CATALOG,
  DECREASE,
  INCREASE,
  type ViewProductRequest,
} from '../../../../driver/port/dtos/ViewProductRequest.js';
import type { ViewProductResponse } from '../../../../driver/port/dtos/ViewProductResponse.js';
import { UseCaseResult } from '../../shared/use-case-result.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import { BaseMyShopUseCase } from './base/BaseMyShopUseCase.js';
import { ViewProductVerification } from './ViewProductVerification.js';

/** SKUs are aliases, resolved to the values the ERP was given. */
export class ViewProduct extends BaseMyShopUseCase<ViewProductResponse, ViewProductVerification> {
  private _skuAlias = '';
  private _fromCatalog: boolean | null = null;
  private _cartSkuAlias: string | null = null;
  private _cartQuantity: string | null = null;
  private _quantity: string | null = null;
  private _quantityStep: string | null = null;
  private _addToCart: boolean | null = null;
  private _followUp: string | null = null;

  constructor(driver: MyShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  sku(skuAlias: string): this {
    this._skuAlias = skuAlias;
    return this;
  }

  fromCatalog(): this {
    this._fromCatalog = true;
    return this;
  }

  cartLine(skuAlias: string, quantity: string): this {
    this._cartSkuAlias = skuAlias;
    this._cartQuantity = quantity;
    return this;
  }

  quantity(quantity: string | null): this {
    this._quantity = quantity;
    return this;
  }

  increaseQuantity(): this {
    this._quantityStep = INCREASE;
    return this;
  }

  decreaseQuantity(): this {
    this._quantityStep = DECREASE;
    return this;
  }

  addToCart(): this {
    this._addToCart = true;
    return this;
  }

  backToCatalog(): this {
    this._followUp = BACK_TO_CATALOG;
    return this;
  }

  /** The driver request this use case sends. */
  private toRequest(): ViewProductRequest {
    return {
      sku: this.context.getParamValue(this._skuAlias),
      fromCatalog: this._fromCatalog,
      cartSku: this._cartSkuAlias === null ? null : this.context.getParamValue(this._cartSkuAlias),
      cartQuantity: this._cartQuantity,
      quantity: this._quantity,
      quantityStep: this._quantityStep,
      addToCart: this._addToCart,
      followUp: this._followUp,
    };
  }

  async execute(): Promise<UseCaseResult<ViewProductResponse, ViewProductVerification>> {
    const result = await this.driver.viewProduct(this.toRequest());

    return new UseCaseResult(result, this.context, (response, ctx) => new ViewProductVerification(response, ctx));
  }
}
