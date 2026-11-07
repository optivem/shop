export interface ThenProduct extends PromiseLike<void> {
  hasSku(sku: string): this;
  hasPrice(price: number): this;
  hasTitle(title: string): this;
  hasDescription(description: string): this;
  hasCategory(category: string): this;
  hasBrand(brand: string): this;
}
