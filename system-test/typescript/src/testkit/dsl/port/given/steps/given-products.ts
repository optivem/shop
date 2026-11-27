import type { GivenStage } from '../given-stage.js';
import type { WhenStage } from '../../when/when-stage.js';
import type { ThenStage } from '../../then/then-stage.js';

/** The ERP's enumerable product list, in the order the products are added. */
export interface GivenProducts {
  withProduct(sku: string, unitPrice: string | number): GivenProducts;
  withProduct(sku: string, title: string, brand: string, category: string, unitPrice: string, description: string): GivenProducts;
  /** The photo the ERP gives a product already added, as the ERP's own path (such as /erp/images/HP-15.jpg). */
  withImageUrl(sku: string, imageUrl: string): GivenProducts;
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
