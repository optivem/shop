import type { MyShopDriver } from '../../../../driver/port/my-shop-driver.js';
import {
  BRAND_SEPARATOR,
  CLEAR_FILTERS,
  REOPEN_PAGE_ADDRESS,
  type BrowseCatalogRequest,
} from '../../../../driver/port/dtos/BrowseCatalogRequest.js';
import type { BrowseCatalogResponse } from '../../../../driver/port/dtos/BrowseCatalogResponse.js';
import { UseCaseResult } from '../../shared/use-case-result.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import { BaseMyShopUseCase } from './base/BaseMyShopUseCase.js';
import { BrowseCatalogVerification } from './BrowseCatalogVerification.js';

export class BrowseCatalog extends BaseMyShopUseCase<BrowseCatalogResponse, BrowseCatalogVerification> {
  private _headerCategory: string | null = null;
  private _search: string | null = null;
  private _category: string | null = null;
  private _brands: string[] | null = null;
  private _sort: string | null = null;
  private _followUp: string | null = null;

  constructor(driver: MyShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  headerCategory(headerCategory: string | null): this {
    this._headerCategory = headerCategory;
    return this;
  }

  search(search: string | null): this {
    this._search = search;
    return this;
  }

  category(category: string | null): this {
    this._category = category;
    return this;
  }

  brands(brands: string[] | null): this {
    this._brands = brands;
    return this;
  }

  sort(sort: string | null): this {
    this._sort = sort;
    return this;
  }

  reopenPageAddress(): this {
    this._followUp = REOPEN_PAGE_ADDRESS;
    return this;
  }

  clearFilters(): this {
    this._followUp = CLEAR_FILTERS;
    return this;
  }

  /** The driver request this use case sends. */
  private toRequest(): BrowseCatalogRequest {
    return {
      headerCategory: this._headerCategory,
      search: this._search,
      category: this._category,
      brands: this._brands === null ? null : this._brands.join(BRAND_SEPARATOR),
      sort: this._sort,
      followUp: this._followUp,
    };
  }

  async execute(): Promise<UseCaseResult<BrowseCatalogResponse, BrowseCatalogVerification>> {
    const result = await this.driver.browseCatalog(this.toRequest());

    return new UseCaseResult(result, this.context, (response, ctx) => new BrowseCatalogVerification(response, ctx));
  }
}
