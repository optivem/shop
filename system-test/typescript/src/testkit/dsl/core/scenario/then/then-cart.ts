import { expect } from '@playwright/test';
import { ManageCart } from '../../usecase/usecases/ManageCart.js';
import type { ManageCartVerification } from '../../usecase/usecases/ManageCartVerification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import { arrangeGiven } from '../given/arrange-given.js';
import type { ScenarioContext } from '../scenario-context.js';

const CHECKOUT_NOTE = 'Tax and discounts are calculated at checkout.';

/** The visitor's one action on the cart, set on the use case. */
export type CartAction = (manageCart: ManageCart) => ManageCart;

/** What the When step asked the visitor to do with the cart. */
export interface CartManagement {
  cartLines: { skuAlias: string; quantity: string }[];
  /** After the cart is filled, the ERP's price of this product changes. */
  changedPrice: { skuAlias: string; unitPrice: string } | null;
  erpUnavailable: boolean;
  viewedWhileErpUnavailable: boolean;
  action: CartAction;
}

type CartAssertion = (verification: ManageCartVerification) => void | Promise<void>;

/**
 * Manages the cart. The cart shows only the products the scenario staged, so only the declared givens are arranged
 * (no default product or tax rate). When the ERP changes after the cart is filled, filling the cart and the action
 * are separate calls in one browser session.
 */
export class ThenCartResultStage implements PromiseLike<void> {
  private readonly _assertions: CartAssertion[] = [];
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
    private readonly management: CartManagement,
  ) {}

  shouldSucceed(): ThenCartSuccess {
    return new ThenCartSuccess(this);
  }

  _addAssertion(fn: CartAssertion): void {
    this._assertions.push(fn);
  }

  private async execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _doExecute(): Promise<void> {
    await arrangeGiven(this.app, this.ctx, this.useCaseContext, { declaredOnly: true });

    const { changedPrice, erpUnavailable, viewedWhileErpUnavailable } = this.management;
    const erpChanges = changedPrice !== null || erpUnavailable || viewedWhileErpUnavailable;
    const manageCart = this.newManageCart();
    if (erpChanges) {
      const fillCart = this.newManageCart();
      for (const line of this.management.cartLines) fillCart.cartLine(line.skuAlias, line.quantity);
      (await fillCart.execute()).shouldSucceed();
      await this.changeErp();
      manageCart.continueSession();
    } else {
      for (const line of this.management.cartLines) manageCart.cartLine(line.skuAlias, line.quantity);
    }
    const verification = (await this.management.action(manageCart).execute()).shouldSucceed();
    for (const fn of this._assertions) await fn(verification);
  }

  private newManageCart(): ManageCart {
    return new ManageCart(this.app.myShop('dynamic'), this.useCaseContext);
  }

  private async changeErp(): Promise<void> {
    const { changedPrice, erpUnavailable, viewedWhileErpUnavailable } = this.management;
    if (changedPrice !== null) {
      await this.changeProductPrice(changedPrice.skuAlias, changedPrice.unitPrice);
    }
    if (viewedWhileErpUnavailable) {
      await this.takeErpDown();
      (await this.newManageCart().continueSession().viewCart().execute()).shouldSucceed();
      const back = await this.app.erpDriver.returnsAvailable();
      expect(back.success, `Failed to bring the ERP back: ${JSON.stringify(back)}`).toBe(true);
    }
    if (erpUnavailable) {
      await this.takeErpDown();
    }
  }

  /** Stages the product lists again, with this product at the new price; a list without the product is left as it is. */
  private async changeProductPrice(skuAlias: string, unitPrice: string): Promise<void> {
    if (this.ctx.productListConfigs.length === 0) {
      throw new Error('No ERP product list was staged, so no price can change');
    }
    for (const productList of this.ctx.productListConfigs) {
      if (!productList.some((product) => product.sku === skuAlias)) continue;
      const products = productList.map((product) => ({
        ...product,
        sku: this.useCaseContext.getParamValue(product.sku),
        price: product.sku === skuAlias ? unitPrice : product.price,
      }));
      const staged = await this.app.erpDriver.returnsProducts({ products });
      if (!staged.success) {
        throw new Error(`Failed to stage the ERP product list: ${staged.error.message}`);
      }
    }
  }

  private async takeErpDown(): Promise<void> {
    const outage = await this.app.erpDriver.returnsUnavailable();
    if (!outage.success) {
      throw new Error(`Failed to make the ERP unavailable: ${outage.error.message}`);
    }
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.execute().then(onfulfilled, onrejected);
  }
}

export class ThenCartSuccess implements PromiseLike<void> {
  constructor(private readonly stage: ThenCartResultStage) {}

  and(): this {
    return this;
  }

  cart(): ThenCart {
    return new ThenCart(this.stage);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenCart implements PromiseLike<void> {
  constructor(private readonly stage: ThenCartResultStage) {}

  private add(fn: CartAssertion): this {
    this.stage._addAssertion(fn);
    return this;
  }

  and(): this {
    return this;
  }

  showsMessage(expectedMessage: string): this {
    return this.add((v) => void v.message(expectedMessage));
  }

  hasCartBadge(expectedCount: number): this {
    return this.add((v) => void v.cartItemCount(expectedCount));
  }

  hasNoCartBadge(): this {
    return this.add((v) => void v.noCartBadge());
  }

  hasLines(...skuAliases: string[]): this {
    return this.add((v) => void v.lines(...skuAliases));
  }

  hasLine(skuAlias: string, expectedTitle: string, expectedUnitPrice: string, expectedQuantity: number, expectedLineTotal: string): this {
    return this.add((v) => void v.line(skuAlias, expectedTitle, expectedUnitPrice, expectedQuantity, expectedLineTotal));
  }

  hasLineQuantity(skuAlias: string, expectedQuantity: number): this {
    return this.add((v) => void v.lineQuantity(skuAlias, expectedQuantity));
  }

  hasLineTotal(skuAlias: string, expectedLineTotal: string): this {
    return this.add((v) => void v.lineTotal(skuAlias, expectedLineTotal));
  }

  hasLinePrices(skuAlias: string, expectedUnitPrice: string, expectedLineTotal: string): this {
    return this.add((v) => void v.linePrices(skuAlias, expectedUnitPrice, expectedLineTotal));
  }

  hasItemCount(expectedCount: number): this {
    return this.add((v) => void v.itemCount(expectedCount));
  }

  hasSubtotal(expectedSubtotal: string): this {
    return this.add((v) => void v.subtotal(expectedSubtotal));
  }

  saysTaxAndDiscountsAreCalculatedAtCheckout(): this {
    return this.add((v) => void v.checkoutNote(CHECKOUT_NOTE));
  }

  offersStartShopping(): this {
    return this.add((v) => void v.startShoppingOffered());
  }

  didNotAskToSignIn(): this {
    return this.add((v) => void v.notAskedToSignIn());
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}
