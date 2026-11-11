import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { ThenCartResultStage, type CartAction, type CartManagement } from '../then/then-cart.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class WhenManageCart {
  private readonly management: CartManagement = {
    cartLines: [],
    changedPrice: null,
    erpUnavailable: false,
    viewedWhileErpUnavailable: false,
    action: (manageCart) => manageCart.viewCart(),
  };

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  withCartContaining(skuAlias: string, quantity: number): this {
    this.management.cartLines.push({ skuAlias, quantity: String(quantity) });
    return this;
  }

  withErpPriceChanged(skuAlias: string, unitPrice: string): this {
    this.management.changedPrice = { skuAlias, unitPrice };
    return this;
  }

  withErpUnavailable(): this {
    this.management.erpUnavailable = true;
    return this;
  }

  withCartViewedWhileErpUnavailable(): this {
    this.management.viewedWhileErpUnavailable = true;
    return this;
  }

  withCatalogBrowsed(): this {
    return this.withAction((manageCart) => manageCart.browseCatalog());
  }

  withAddedFromCatalog(skuAlias: string): this {
    return this.withAction((manageCart) => manageCart.addFromCatalog(skuAlias));
  }

  withAddedFromProductPage(skuAlias: string, quantity: string): this {
    return this.withAction((manageCart) => manageCart.addFromProductPage(skuAlias, quantity));
  }

  withQuantityChanged(skuAlias: string, quantity: string): this {
    return this.withAction((manageCart) => manageCart.changeQuantity(skuAlias, quantity));
  }

  withQuantityIncreased(skuAlias: string): this {
    return this.withAction((manageCart) => manageCart.increaseQuantity(skuAlias));
  }

  withQuantityDecreased(skuAlias: string): this {
    return this.withAction((manageCart) => manageCart.decreaseQuantity(skuAlias));
  }

  withLineRemoved(skuAlias: string): this {
    return this.withAction((manageCart) => manageCart.removeLine(skuAlias));
  }

  withSiteLeftAndRevisited(): this {
    return this.withAction((manageCart) => manageCart.revisit());
  }

  withSignedInAsCustomer(): this {
    return this.withAction((manageCart) => manageCart.signIn());
  }

  private withAction(action: CartAction): this {
    this.management.action = action;
    return this;
  }

  then(): ThenCartResultStage;
  then(...args: unknown[]): ThenCartResultStage {
    assertNotAwaited(args);
    this.ctx.markExecuted();
    return new ThenCartResultStage(this.app, this.ctx, this.useCaseContext, {
      ...this.management,
      cartLines: [...this.management.cartLines],
    });
  }
}
