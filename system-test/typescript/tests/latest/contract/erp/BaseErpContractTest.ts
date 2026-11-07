import type { ContractTest } from '../base/BaseExternalSystemContractTest.js';

export function registerErpContractTests(test: ContractTest): void {
  test('shouldBeAbleToGetProduct', async ({ scenario }) => {
    await scenario
      .given()
      .product()
      .withSku('BOOK-123')
      .withUnitPrice(12)
      .then()
      .product('BOOK-123')
      .hasSku('BOOK-123')
      .hasPrice(12);
  });

  test('shouldBeAbleToGetProductDetails', async ({ scenario }) => {
    await scenario
      .given()
      .product()
      .withSku('BOOK-123')
      .withUnitPrice(12)
      .withTitle('Clean Code')
      .withDescription('A handbook of agile software craftsmanship')
      .withCategory('Books')
      .withBrand('Prentice Hall')
      .then()
      .product('BOOK-123')
      .hasTitle('Clean Code')
      .hasDescription('A handbook of agile software craftsmanship')
      .hasCategory('Books')
      .hasBrand('Prentice Hall');
  });

  test('shouldBeAbleToListProducts', async ({ scenario }) => {
    await scenario
      .given()
      .products()
      .withProduct('BOOK-123', '12.00')
      .withProduct('BOOK-456', '15.50')
      .then()
      .products()
      .containsProduct('BOOK-123', '12.00')
      .containsProduct('BOOK-456', '15.50');
  });
}
