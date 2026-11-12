import type { MyShopDriver } from '../../../../driver/port/my-shop-driver.js';
import { OPEN, PROCEED_FROM_CART, type CheckOutRequest } from '../../../../driver/port/dtos/CheckOutRequest.js';
import type { CheckOutResponse } from '../../../../driver/port/dtos/CheckOutResponse.js';
import type { ShippingAddress } from '../../../../driver/port/dtos/ShippingAddress.js';
import { UseCaseResult } from '../../shared/use-case-result.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import { BaseMyShopUseCase } from './base/BaseMyShopUseCase.js';
import { CheckOutVerification } from './CheckOutVerification.js';

interface CartLineEntry {
  skuAlias: string;
  quantity: string;
}

/**
 * SKUs and coupon codes are aliases, resolved to the values the ERP and the coupon were given. The shipping address
 * is literal: its country is a code the checkout's form offers. Without placeOrder(), the checkout is only opened
 * (and the address entered).
 */
export class CheckOut extends BaseMyShopUseCase<CheckOutResponse, CheckOutVerification> {
  private readonly _cartLines: CartLineEntry[] = [];
  private _continueSession: boolean | null = null;
  private _entry = OPEN;
  private _signIn: boolean | null = null;
  private _shippingAddress: ShippingAddress | null = null;
  private _saveAsDefault: boolean | null = null;
  private _couponCodeAlias: string | null = null;
  private _placeOrder: boolean | null = null;

  constructor(driver: MyShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  /** A line the cart gets first, added from its product page; lines are added in the order given. */
  cartLine(skuAlias: string, quantity: string): this {
    this._cartLines.push({ skuAlias, quantity });
    return this;
  }

  /** Keeps the browser session (and its cart) of the previous call instead of starting a new one. */
  continueSession(): this {
    this._continueSession = true;
    return this;
  }

  proceedFromCart(): this {
    this._entry = PROCEED_FROM_CART;
    return this;
  }

  signIn(): this {
    this._signIn = true;
    return this;
  }

  shippingAddress(firstName: string, lastName: string, street: string, postalCode: string, city: string, country: string): this {
    this._shippingAddress = { firstName, lastName, street, postalCode, city, country };
    return this;
  }

  /** Unticks "Save as my default shipping address", which a first order offers ticked. */
  doNotSaveAsDefault(): this {
    this._saveAsDefault = false;
    return this;
  }

  couponCode(couponCodeAlias: string): this {
    this._couponCodeAlias = couponCodeAlias;
    return this;
  }

  placeOrder(): this {
    this._placeOrder = true;
    return this;
  }

  /** The driver request this use case sends. */
  private toRequest(): CheckOutRequest {
    return {
      cartLines: this._cartLines.map((line) => ({
        sku: this.context.getParamValue(line.skuAlias),
        quantity: line.quantity,
      })),
      continueSession: this._continueSession,
      entry: this._entry,
      signIn: this._signIn,
      shippingAddress: this._shippingAddress,
      saveAsDefault: this._saveAsDefault,
      couponCode: this._couponCodeAlias === null ? null : this.context.getParamValue(this._couponCodeAlias),
      placeOrder: this._placeOrder,
    };
  }

  async execute(): Promise<UseCaseResult<CheckOutResponse, CheckOutVerification>> {
    const result = await this.driver.checkOut(this.toRequest());

    return new UseCaseResult(result, this.context, (response, ctx) => new CheckOutVerification(response, ctx));
  }
}
