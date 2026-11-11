import type { MyShopDriver } from '../../../../driver/port/my-shop-driver.js';
import {
  ADD_FROM_CATALOG,
  ADD_FROM_PRODUCT_PAGE,
  BROWSE_CATALOG,
  CHANGE_QUANTITY,
  DECREASE_QUANTITY,
  INCREASE_QUANTITY,
  REMOVE_LINE,
  REVISIT,
  SIGN_IN,
  VIEW_CART,
  type ManageCartRequest,
} from '../../../../driver/port/dtos/ManageCartRequest.js';
import type { ManageCartResponse } from '../../../../driver/port/dtos/ManageCartResponse.js';
import { UseCaseResult } from '../../shared/use-case-result.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import { BaseMyShopUseCase } from './base/BaseMyShopUseCase.js';
import { ManageCartVerification } from './ManageCartVerification.js';

interface CartLineEntry {
  skuAlias: string;
  quantity: string;
}

/** SKUs are aliases, resolved to the values the ERP was given. Without an action, only the cart is filled. */
export class ManageCart extends BaseMyShopUseCase<ManageCartResponse, ManageCartVerification> {
  private readonly _cartLines: CartLineEntry[] = [];
  private _continueSession: boolean | null = null;
  private _action: string | null = null;
  private _skuAlias: string | null = null;
  private _quantity: string | null = null;

  constructor(driver: MyShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  /** A line the cart gets first, added from its product page; lines are added in the order given. */
  cartLine(skuAlias: string, quantity: string): this {
    this._cartLines.push({ skuAlias, quantity });
    return this;
  }

  /** Keeps the browser session of the previous call instead of starting a new one. */
  continueSession(): this {
    this._continueSession = true;
    return this;
  }

  viewCart(): this {
    return this.action(VIEW_CART, null, null);
  }

  browseCatalog(): this {
    return this.action(BROWSE_CATALOG, null, null);
  }

  addFromCatalog(skuAlias: string): this {
    return this.action(ADD_FROM_CATALOG, skuAlias, null);
  }

  addFromProductPage(skuAlias: string, quantity: string): this {
    return this.action(ADD_FROM_PRODUCT_PAGE, skuAlias, quantity);
  }

  changeQuantity(skuAlias: string, quantity: string): this {
    return this.action(CHANGE_QUANTITY, skuAlias, quantity);
  }

  increaseQuantity(skuAlias: string): this {
    return this.action(INCREASE_QUANTITY, skuAlias, null);
  }

  decreaseQuantity(skuAlias: string): this {
    return this.action(DECREASE_QUANTITY, skuAlias, null);
  }

  removeLine(skuAlias: string): this {
    return this.action(REMOVE_LINE, skuAlias, null);
  }

  revisit(): this {
    return this.action(REVISIT, null, null);
  }

  signIn(): this {
    return this.action(SIGN_IN, null, null);
  }

  private action(action: string, skuAlias: string | null, quantity: string | null): this {
    this._action = action;
    this._skuAlias = skuAlias;
    this._quantity = quantity;
    return this;
  }

  /** The driver request this use case sends. */
  private toRequest(): ManageCartRequest {
    return {
      cartLines: this._cartLines.map((line) => ({
        sku: this.context.getParamValue(line.skuAlias),
        quantity: line.quantity,
      })),
      continueSession: this._continueSession,
      action: this._action,
      sku: this._skuAlias === null ? null : this.context.getParamValue(this._skuAlias),
      quantity: this._quantity,
    };
  }

  async execute(): Promise<UseCaseResult<ManageCartResponse, ManageCartVerification>> {
    const result = await this.driver.manageCart(this.toRequest());

    return new UseCaseResult(result, this.context, (response, ctx) => new ManageCartVerification(response, ctx));
  }
}
