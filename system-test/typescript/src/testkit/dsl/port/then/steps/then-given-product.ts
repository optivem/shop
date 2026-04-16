export interface ThenGivenProduct extends PromiseLike<void> {
  hasSku(sku: string): this;
  hasPrice(price: number): this;
}
