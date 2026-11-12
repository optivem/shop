import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { ThenCheckoutResultStage, type CheckoutStep, type CheckoutSteps } from '../then/then-checkout.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class WhenCheckOut {
  private readonly checkout: CheckoutSteps = {
    cartLines: [],
    changedPrice: null,
    withdrawnSkuAlias: null,
    steps: [],
  };

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  withCartContaining(skuAlias: string, quantity: number): this {
    this.checkout.cartLines.push({ skuAlias, quantity: String(quantity) });
    return this;
  }

  withErpPriceChanged(skuAlias: string, unitPrice: string): this {
    this.checkout.changedPrice = { skuAlias, unitPrice };
    return this;
  }

  withErpNoLongerOffering(skuAlias: string): this {
    this.checkout.withdrawnSkuAlias = skuAlias;
    return this;
  }

  withProceededFromCart(): this {
    return this.step((checkOut) => checkOut.proceedFromCart());
  }

  withProceededFromCartAndSignedIn(): this {
    return this.step((checkOut) => checkOut.proceedFromCart().signIn());
  }

  withShippingAddress(firstName: string, lastName: string, street: string, postalCode: string, city: string, country: string): this {
    return this.step((checkOut) => checkOut.shippingAddress(firstName, lastName, street, postalCode, city, country));
  }

  withSaveAsDefaultUnticked(): this {
    return this.step((checkOut) => checkOut.doNotSaveAsDefault());
  }

  withCouponCode(couponCode: string): this {
    return this.step((checkOut) => checkOut.couponCode(couponCode));
  }

  withOrderPlaced(): this {
    return this.step((checkOut) => checkOut.placeOrder());
  }

  private step(step: CheckoutStep): this {
    this.checkout.steps.push(step);
    return this;
  }

  then(): ThenCheckoutResultStage;
  then(...args: unknown[]): ThenCheckoutResultStage {
    assertNotAwaited(args);
    this.ctx.markExecuted();
    return new ThenCheckoutResultStage(this.app, this.ctx, this.useCaseContext, {
      ...this.checkout,
      cartLines: [...this.checkout.cartLines],
      steps: [...this.checkout.steps],
    });
  }
}
