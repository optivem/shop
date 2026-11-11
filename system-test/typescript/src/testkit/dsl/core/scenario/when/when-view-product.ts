import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { ThenProductPageResultStage, type ProductView } from '../then/then-product-page.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class WhenViewProduct {
  private readonly view: ProductView = {
    sku: '',
    fromCatalog: false,
    cartSku: null,
    cartQuantity: null,
    quantity: null,
    increase: false,
    decrease: false,
    addToCart: false,
    backToCatalog: false,
  };

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  withSku(skuAlias: string): this {
    this.view.sku = skuAlias;
    return this;
  }

  fromCatalog(): this {
    this.view.fromCatalog = true;
    return this;
  }

  withCartContaining(skuAlias: string, quantity: number): this {
    this.view.cartSku = skuAlias;
    this.view.cartQuantity = String(quantity);
    return this;
  }

  withQuantity(quantity: string): this {
    this.view.quantity = quantity;
    return this;
  }

  withQuantityIncreased(): this {
    this.view.increase = true;
    return this;
  }

  withQuantityDecreased(): this {
    this.view.decrease = true;
    return this;
  }

  withAddedToCart(quantity: string): this {
    this.view.quantity = quantity;
    this.view.addToCart = true;
    return this;
  }

  withBackToCatalogFollowed(): this {
    this.view.backToCatalog = true;
    return this;
  }

  then(): ThenProductPageResultStage;
  then(...args: unknown[]): ThenProductPageResultStage {
    assertNotAwaited(args);
    this.ctx.markExecuted();
    return new ThenProductPageResultStage(this.app, this.ctx, this.useCaseContext, { ...this.view });
  }
}
