import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { ThenCatalogResultStage, type CatalogBrowse } from '../then/then-catalog.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class WhenBrowseCatalog {
  private readonly browse: CatalogBrowse = {
    headerCategory: null,
    search: null,
    category: null,
    brands: null,
    sort: null,
    reopenPageAddress: false,
    clearFilters: false,
  };

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  withHeaderCategoryLink(category: string): this {
    this.browse.headerCategory = category;
    return this;
  }

  withSearch(term: string): this {
    this.browse.search = term;
    return this;
  }

  withCategory(category: string): this {
    this.browse.category = category;
    return this;
  }

  withBrands(...brands: string[]): this {
    this.browse.brands = brands;
    return this;
  }

  withSort(sort: string): this {
    this.browse.sort = sort;
    return this;
  }

  withPageAddressReopened(): this {
    this.browse.reopenPageAddress = true;
    return this;
  }

  withFiltersCleared(): this {
    this.browse.clearFilters = true;
    return this;
  }

  then(): ThenCatalogResultStage;
  then(...args: unknown[]): ThenCatalogResultStage {
    assertNotAwaited(args);
    this.ctx.markExecuted();
    return new ThenCatalogResultStage(this.app, this.ctx, this.useCaseContext, { ...this.browse });
  }
}
