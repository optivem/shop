import { expect } from '@playwright/test';
import { ViewProduct } from '../../usecase/usecases/ViewProduct.js';
import type { ViewProductVerification } from '../../usecase/usecases/ViewProductVerification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import { arrangeGiven } from '../given/arrange-given.js';
import type { ScenarioContext } from '../scenario-context.js';

/** What the When step asked the visitor to do on the product page. */
export interface ProductView {
  sku: string;
  fromCatalog: boolean;
  cartSku: string | null;
  cartQuantity: string | null;
  quantity: string | null;
  increase: boolean;
  decrease: boolean;
  addToCart: boolean;
  backToCatalog: boolean;
}

type ProductPageAssertion = (verification: ViewProductVerification) => void | Promise<void>;

/**
 * Views a product. The product page shows only the products the scenario staged, so only the declared givens are
 * arranged (no default product or tax rate).
 */
export class ThenProductPageResultStage implements PromiseLike<void> {
  private _expectSuccess = true;
  private readonly _assertions: ProductPageAssertion[] = [];
  private readonly _errorMessages: string[] = [];
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
    private readonly view: ProductView,
  ) {}

  shouldSucceed(): ThenProductPageSuccess {
    this._expectSuccess = true;
    return new ThenProductPageSuccess(this);
  }

  shouldFail(): ThenProductPageFailure {
    this._expectSuccess = false;
    return new ThenProductPageFailure(this);
  }

  _addAssertion(fn: ProductPageAssertion): void {
    this._assertions.push(fn);
  }

  _addErrorMessage(expected: string): void {
    this._errorMessages.push(expected);
  }

  /** Opens the acting user's order history on the same channel. */
  async _browseOrderHistory(): Promise<void> {
    const result = await this.app.myShop('dynamic').browseOrderHistory({ orderNumber: '' });
    expect(result.success, JSON.stringify(result)).toBe(true);
  }

  private async execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _doExecute(): Promise<void> {
    await arrangeGiven(this.app, this.ctx, this.useCaseContext, { declaredOnly: true });

    const viewProduct = new ViewProduct(this.app.myShop('dynamic'), this.useCaseContext)
      .sku(this.view.sku)
      .quantity(this.view.quantity);
    if (this.view.fromCatalog) viewProduct.fromCatalog();
    if (this.view.cartSku !== null) viewProduct.cartLine(this.view.cartSku, this.view.cartQuantity ?? '1');
    if (this.view.increase) viewProduct.increaseQuantity();
    if (this.view.decrease) viewProduct.decreaseQuantity();
    if (this.view.addToCart) viewProduct.addToCart();
    if (this.view.backToCatalog) viewProduct.backToCatalog();
    const result = await viewProduct.execute();

    if (this._expectSuccess) {
      const verification = result.shouldSucceed();
      for (const fn of this._assertions) await fn(verification);
    } else {
      const errorVerification = result.shouldFail();
      for (const expected of this._errorMessages) errorVerification.errorMessage(expected);
    }
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.execute().then(onfulfilled, onrejected);
  }
}

export class ThenProductPageSuccess implements PromiseLike<void> {
  constructor(private readonly stage: ThenProductPageResultStage) {}

  and(): this {
    return this;
  }

  productPage(): ThenProductPage {
    return new ThenProductPage(this.stage);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenProductPageFailure implements PromiseLike<void> {
  constructor(private readonly stage: ThenProductPageResultStage) {}

  and(): this {
    return this;
  }

  errorMessage(expectedMessage: string): this {
    this.stage._addErrorMessage(expectedMessage);
    return this;
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenProductPage implements PromiseLike<void> {
  constructor(private readonly stage: ThenProductPageResultStage) {}

  private add(fn: ProductPageAssertion): this {
    this.stage._addAssertion(fn);
    return this;
  }

  and(): this {
    return this;
  }

  hasTitle(expectedTitle: string): this {
    return this.add((v) => void v.title(expectedTitle));
  }

  hasBrand(expectedBrand: string): this {
    return this.add((v) => void v.brand(expectedBrand));
  }

  hasCategory(expectedCategory: string): this {
    return this.add((v) => void v.category(expectedCategory));
  }

  hasSku(expectedSkuAlias: string): this {
    return this.add((v) => void v.sku(expectedSkuAlias));
  }

  hasPrice(expectedPrice: string): this {
    return this.add((v) => void v.price(expectedPrice));
  }

  hasDescription(expectedDescription: string): this {
    return this.add((v) => void v.description(expectedDescription));
  }

  hasBreadcrumbs(...expectedBreadcrumbs: string[]): this {
    return this.add((v) => void v.breadcrumbs(...expectedBreadcrumbs));
  }

  hasQuantity(expectedQuantity: number): this {
    return this.add((v) => void v.quantity(expectedQuantity));
  }

  showsMessage(expectedMessage: string): this {
    return this.add((v) => void v.message(expectedMessage));
  }

  offersBackToCatalog(): this {
    return this.add((v) => void v.backToCatalogOffered());
  }

  doesNotOfferAddToCart(): this {
    return this.add((v) => void v.addToCartNotOffered());
  }

  hasCartBadge(expectedCount: number): this {
    return this.add((v) => void v.cartItemCount(expectedCount));
  }

  isOnPageOfProduct(skuAlias: string): this {
    return this.add((v) => void v.onPageOfProduct(skuAlias));
  }

  cartContains(skuAlias: string, expectedQuantity: number): this {
    return this.add((v) => void v.cartContains(skuAlias, expectedQuantity));
  }

  cartIsEmpty(): this {
    return this.add((v) => void v.cartIsEmpty());
  }

  catalogShowsProducts(...skuAliases: string[]): this {
    return this.add((v) => void v.catalogShowsProducts(...skuAliases));
  }

  canOpenMyOrders(): this {
    return this.add(() => this.stage._browseOrderHistory());
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}
