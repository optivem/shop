import { test, forChannels } from './fixtures.js';

forChannels('ui', 'api')(() => {
    const nonIntegerQuantities = ['3.5', 'lala', 'invalid-quantity'];

    nonIntegerQuantities.forEach((qty) => {
        test('shouldRejectOrderWithNonIntegerQuantity_' + qty, async ({ scenario }) => {
            await scenario
                .when()
                .placeOrder()
                .withQuantity(qty)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('quantity', 'Quantity must be an integer');
        });
    });

    test('shouldRejectOrderForNonExistentProduct', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withSku('NON-EXISTENT-SKU-12345')
            .withQuantity(1)
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('sku', 'Product does not exist for SKU: NON-EXISTENT-SKU-12345');
    });

    const emptySkus = ['', '   '];

    emptySkus.forEach((sku) => {
        test('shouldRejectOrderWithEmptySku_"' + sku + '"', async ({ scenario }) => {
            await scenario
                .when()
                .placeOrder()
                .withSku(sku)
                .withQuantity(1)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('sku', 'SKU must not be empty');
        });
    });

    const nonPositiveQuantities = ['-10', '-1', '0'];

    nonPositiveQuantities.forEach((qty) => {
        test('shouldRejectOrderWithNonPositiveQuantity_' + qty, async ({ scenario }) => {
            await scenario
                .when()
                .placeOrder()
                .withQuantity(qty)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('quantity', 'Quantity must be positive');
        });
    });

    const emptyQuantities = ['', '   '];

    emptyQuantities.forEach((qty) => {
        test('shouldRejectOrderWithEmptyQuantity_"' + qty + '"', async ({ scenario }) => {
            await scenario
                .when()
                .placeOrder()
                .withQuantity(qty)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('quantity', 'Quantity must not be empty');
        });
    });
});

forChannels('api')(() => {
    test('shouldRejectOrderWithNullQuantity', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withQuantity(null)
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('quantity', 'Quantity must not be empty');
    });
});
