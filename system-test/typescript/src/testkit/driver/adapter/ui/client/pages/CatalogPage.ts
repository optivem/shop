import type { Locator } from '@playwright/test';
import type { BrowseCatalogResponse, CatalogProductDto } from '../../../../port/dtos/BrowseCatalogResponse.js';
import { BasePage, PAGE_TIMEOUT_MS } from './BasePage.js';
import { ProductPage } from './ProductPage.js';

const CATALOG_SELECTOR = "section[aria-label='Catalog']";
const PRODUCT_LIST_SELECTOR = `${CATALOG_SELECTOR} [aria-label='Product List']`;
const CLEAR_FILTERS_SELECTOR = `${CATALOG_SELECTOR} [aria-label='Clear Filters']`;
const UNAVAILABLE_SELECTOR = `${CATALOG_SELECTOR} .alert-danger`;
const NO_MATCH_SELECTOR = `${CATALOG_SELECTOR} p.lead`;
/** Exactly one of these shows once the catalog has loaded: products, the no-match notice or the ERP outage. */
export const CATALOG_READY_SELECTOR = `${CATALOG_SELECTOR} :is([aria-label='Product List'], [aria-label='Clear Filters'], .alert-danger)`;
const PRODUCT_CARD_SELECTOR = `${PRODUCT_LIST_SELECTOR} article[aria-label^='Product ']`;
const PRODUCT_CARD_LABEL_PREFIX = 'Product ';
const PRODUCT_COUNT_SELECTOR = `${CATALOG_SELECTOR} [aria-label='Product Count']`;
const SORT_SELECTOR = `${CATALOG_SELECTOR} select[aria-label='Sort']`;
const CATEGORY_FILTER_SELECTOR = "fieldset[aria-label='Category Filter']";
const BRAND_FILTER_SELECTOR = "fieldset[aria-label='Brand Filter']";
const CHECKED_OPTION_SELECTOR = ' .form-check:has(input:checked) label';
const SEARCH_INPUT_SELECTOR = "input[aria-label='Search Products']";
const SEARCH_BUTTON_SELECTOR = "button[aria-label='Search']";
const SEARCH_PARAMETER = 'q=';

/** The parts of a DOM select element the sort reader uses (the test project has no DOM typings). */
interface SelectElement {
  options: ArrayLike<{ text: string } | undefined>;
  selectedIndex: number;
}

/** The catalog on the home page `/` (UC-006): header search and category links, filters, sort and the product grid. */
export class CatalogPage extends BasePage {
  async waitForLoaded(): Promise<void> {
    await this.page.locator(CATALOG_READY_SELECTOR).waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
  }

  async clickHeaderCategory(category: string): Promise<void> {
    await this.changeView(() =>
      this.page.locator(`nav[aria-label='Store navigation'] a:text-is('${category}')`).click({ timeout: PAGE_TIMEOUT_MS }),
    );
  }

  /** An empty search on an unsearched catalog shows the same page address, so there is no change to wait for. */
  async search(term: string): Promise<void> {
    const sameAddress = term.trim() === '' && !this.page.url().includes(SEARCH_PARAMETER);
    await this.page.locator(SEARCH_INPUT_SELECTOR).fill(term, { timeout: PAGE_TIMEOUT_MS });
    const submit = () => this.page.locator(SEARCH_BUTTON_SELECTOR).click({ timeout: PAGE_TIMEOUT_MS });
    if (sameAddress) {
      await submit();
      await this.waitForLoaded();
      return;
    }
    await this.changeView(submit);
  }

  async selectCategory(category: string): Promise<void> {
    if (category === (await this.readSelectedCategory())) return;
    await this.changeView(() => this.clickOptionLabel(CATEGORY_FILTER_SELECTOR, category));
  }

  async tickBrand(brand: string): Promise<void> {
    if ((await this.readTickedBrands()).includes(brand)) return;
    await this.changeView(() => this.clickOptionLabel(BRAND_FILTER_SELECTOR, brand));
  }

  async sortBy(sortLabel: string): Promise<void> {
    if (sortLabel === (await this.readSortLabel())) return;
    await this.changeView(async () => {
      await this.page.locator(SORT_SELECTOR).selectOption({ label: sortLabel }, { timeout: PAGE_TIMEOUT_MS });
    });
  }

  async reopenPageAddress(): Promise<void> {
    await this.page.goto(this.page.url());
    await this.waitForLoaded();
  }

  /** Opens a product from its card, by the title link. */
  async clickProduct(sku: string): Promise<ProductPage> {
    const previousUrl = this.page.url();
    await this.page
      .locator(`${PRODUCT_LIST_SELECTOR} article[aria-label='${PRODUCT_CARD_LABEL_PREFIX}${sku}'] a[aria-label='Product Title']`)
      .click({ timeout: PAGE_TIMEOUT_MS });
    await this.page.waitForURL((url) => url.toString() !== previousUrl, { timeout: PAGE_TIMEOUT_MS });
    return new ProductPage(this.page);
  }

  /** Adds a product with its card's "Add to cart" (quantity 1) and returns the confirmation or the error. */
  async clickAddToCart(sku: string): Promise<string> {
    await this.page
      .locator(`${PRODUCT_LIST_SELECTOR} button[aria-label='Add ${sku} to Cart']`)
      .click({ timeout: PAGE_TIMEOUT_MS });
    const result = await this.getResult();
    const message = result.success ? result.value : result.error.message;
    await this.waitForLoaded();
    return message.trim();
  }

  async clickClearFilters(): Promise<void> {
    await this.changeView(() => this.page.locator(CLEAR_FILTERS_SELECTOR).click({ timeout: PAGE_TIMEOUT_MS }));
  }

  async read(): Promise<BrowseCatalogResponse> {
    await this.waitForLoaded();
    const cards = await this.page.locator(PRODUCT_CARD_SELECTOR).all();
    const products: CatalogProductDto[] = [];
    for (const card of cards) {
      const label = (await card.getAttribute('aria-label')) ?? '';
      products.push({
        sku: label.substring(PRODUCT_CARD_LABEL_PREFIX.length),
        title: await this.text(card.locator("[aria-label='Product Title']")),
        brand: await this.text(card.locator("[aria-label='Product Brand']")),
        price: await this.text(card.locator("[aria-label='Display Price']")),
      });
    }

    return {
      products,
      productCount: await this.readProductCount(),
      categories: trimmed(await this.page.locator(`${CATEGORY_FILTER_SELECTOR} label`).allTextContents()),
      selectedCategory: await this.readSelectedCategory(),
      brands: trimmed(await this.page.locator(`${BRAND_FILTER_SELECTOR} label`).allTextContents()),
      tickedBrands: await this.readTickedBrands(),
      sort: await this.readSortLabel(),
      message: await this.readMessage(),
      clearFiltersOffered: (await this.page.locator(CLEAR_FILTERS_SELECTOR).count()) > 0,
    };
  }

  /** Runs an action that changes the view, then waits for the new page address and the re-rendered catalog. */
  private async changeView(action: () => Promise<void>): Promise<void> {
    const previousUrl = this.page.url();
    await action();
    await this.page.waitForURL((url) => url.toString() !== previousUrl, { timeout: PAGE_TIMEOUT_MS });
    await this.waitForLoaded();
  }

  private async clickOptionLabel(fieldsetSelector: string, label: string): Promise<void> {
    await this.page.locator(`${fieldsetSelector} label:text-is('${label}')`).click({ timeout: PAGE_TIMEOUT_MS });
  }

  private async readProductCount(): Promise<number> {
    const text = await this.text(this.page.locator(PRODUCT_COUNT_SELECTOR));
    return Number.parseInt(text.split(/\s+/)[0] ?? '', 10);
  }

  private async readSelectedCategory(): Promise<string | null> {
    const selected = trimmed(await this.page.locator(`${CATEGORY_FILTER_SELECTOR}${CHECKED_OPTION_SELECTOR}`).allTextContents());
    return selected[0] ?? null;
  }

  private async readTickedBrands(): Promise<string[]> {
    return trimmed(await this.page.locator(`${BRAND_FILTER_SELECTOR}${CHECKED_OPTION_SELECTOR}`).allTextContents());
  }

  private async readSortLabel(): Promise<string | null> {
    return this.page
      .locator(SORT_SELECTOR)
      .evaluate(
        (select: SelectElement): string | null => select.options[select.selectedIndex]?.text ?? null,
        undefined,
        { timeout: PAGE_TIMEOUT_MS },
      );
  }

  private async readMessage(): Promise<string | null> {
    for (const selector of [UNAVAILABLE_SELECTOR, NO_MATCH_SELECTOR]) {
      const locator = this.page.locator(selector);
      if ((await locator.count()) > 0) return this.text(locator);
    }
    return null;
  }

  private async text(locator: Locator): Promise<string> {
    return ((await locator.textContent({ timeout: PAGE_TIMEOUT_MS })) ?? '').trim();
  }
}

function trimmed(values: string[]): string[] {
  return values.map((value) => value.trim());
}
