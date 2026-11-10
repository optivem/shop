import { expect } from '@playwright/test';
import { BrowseCatalog } from '../../usecase/usecases/BrowseCatalog.js';
import type { BrowseCatalogVerification } from '../../usecase/usecases/BrowseCatalogVerification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import { arrangeGiven } from '../given/arrange-given.js';
import type { ScenarioContext } from '../scenario-context.js';

/** What the When step asked the visitor to do in the catalog. */
export interface CatalogBrowse {
  headerCategory: string | null;
  search: string | null;
  category: string | null;
  brands: string[] | null;
  sort: string | null;
  reopenPageAddress: boolean;
  clearFilters: boolean;
}

type CatalogAssertion = (verification: BrowseCatalogVerification) => void | Promise<void>;

/**
 * Browses the catalog. The catalog lists only the products the scenario staged, so only the declared givens are
 * arranged (no default product or tax rate).
 */
export class ThenCatalogResultStage implements PromiseLike<void> {
  private _expectSuccess = true;
  private readonly _catalogAssertions: CatalogAssertion[] = [];
  private readonly _errorMessages: string[] = [];
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
    private readonly browse: CatalogBrowse,
  ) {}

  shouldSucceed(): ThenCatalogSuccess {
    this._expectSuccess = true;
    return new ThenCatalogSuccess(this);
  }

  shouldFail(): ThenCatalogFailure {
    this._expectSuccess = false;
    return new ThenCatalogFailure(this);
  }

  _addCatalogAssertion(fn: CatalogAssertion): void {
    this._catalogAssertions.push(fn);
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

    const browseCatalog = new BrowseCatalog(this.app.myShop('dynamic'), this.useCaseContext)
      .headerCategory(this.browse.headerCategory)
      .search(this.browse.search)
      .category(this.browse.category)
      .brands(this.browse.brands)
      .sort(this.browse.sort);
    if (this.browse.reopenPageAddress) browseCatalog.reopenPageAddress();
    if (this.browse.clearFilters) browseCatalog.clearFilters();
    const result = await browseCatalog.execute();

    if (this._expectSuccess) {
      const verification = result.shouldSucceed();
      for (const fn of this._catalogAssertions) await fn(verification);
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

export class ThenCatalogSuccess implements PromiseLike<void> {
  constructor(private readonly stage: ThenCatalogResultStage) {}

  and(): this {
    return this;
  }

  catalog(): ThenCatalog {
    return new ThenCatalog(this.stage);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenCatalogFailure implements PromiseLike<void> {
  constructor(private readonly stage: ThenCatalogResultStage) {}

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

export class ThenCatalog implements PromiseLike<void> {
  constructor(private readonly stage: ThenCatalogResultStage) {}

  private add(fn: CatalogAssertion): this {
    this.stage._addCatalogAssertion(fn);
    return this;
  }

  and(): this {
    return this;
  }

  showsProducts(...skuAliases: string[]): this {
    return this.add((v) => void v.showsProducts(...skuAliases));
  }

  showsNoProducts(): this {
    return this.add((v) => void v.showsNoProducts());
  }

  hasProductCount(expectedCount: number): this {
    return this.add((v) => void v.productCount(expectedCount));
  }

  showsProduct(skuAlias: string, expectedTitle: string, expectedBrand: string, expectedPrice: string): this {
    return this.add((v) => void v.product(skuAlias, expectedTitle, expectedBrand, expectedPrice));
  }

  offersCategories(...expectedCategories: string[]): this {
    return this.add((v) => void v.categoryOptions(...expectedCategories));
  }

  hasCategorySelected(expectedCategory: string): this {
    return this.add((v) => void v.selectedCategory(expectedCategory));
  }

  offersBrands(...expectedBrands: string[]): this {
    return this.add((v) => void v.brandOptions(...expectedBrands));
  }

  hasNoBrandTicked(): this {
    return this.add((v) => void v.noBrandTicked());
  }

  hasSort(expectedSort: string): this {
    return this.add((v) => void v.sort(expectedSort));
  }

  showsMessage(expectedMessage: string): this {
    return this.add((v) => void v.message(expectedMessage));
  }

  offersToClearFilters(): this {
    return this.add((v) => void v.clearFiltersOffered());
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
