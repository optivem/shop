import { ReturnsProductNotFound } from '../../usecase/external/erp/usecases/ReturnsProductNotFound.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ProductConfig, ScenarioContext } from '../scenario-context.js';

/** The ERP product lists the scenario staged, so a When step can change them after the cart was filled. */
export class StagedProducts {
  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  /** Stages the product lists again, with this product at the new price; a list without the product is left as it is. */
  async changePrice(skuAlias: string, unitPrice: string): Promise<void> {
    if (this.ctx.productListConfigs.length === 0) {
      throw new Error('No ERP product list was staged, so no price can change');
    }
    for (const productList of this.listsWith(skuAlias)) {
      await this.stage(productList.map((product) => (product.sku === skuAlias ? { ...product, price: unitPrice } : product)));
    }
  }

  /** The ERP no longer offers this product: the lists are staged again without it, and asking for it finds nothing. */
  async withdraw(skuAlias: string): Promise<void> {
    if (this.ctx.productListConfigs.length === 0) {
      throw new Error('No ERP product list was staged, so no product can be withdrawn');
    }
    for (const productList of this.listsWith(skuAlias)) {
      await this.stage(productList.filter((product) => product.sku !== skuAlias));
    }
    (await new ReturnsProductNotFound(this.app.erpDriver, this.useCaseContext).sku(skuAlias).execute()).shouldSucceed();
  }

  private listsWith(skuAlias: string): ProductConfig[][] {
    return this.ctx.productListConfigs.filter((productList) => productList.some((product) => product.sku === skuAlias));
  }

  private async stage(productList: ProductConfig[]): Promise<void> {
    const products = productList.map((product) => ({ ...product, sku: this.useCaseContext.getParamValue(product.sku) }));
    const staged = await this.app.erpDriver.returnsProducts({ products });
    if (!staged.success) {
      throw new Error(`Failed to stage the ERP product list: ${staged.error.message}`);
    }
  }
}
