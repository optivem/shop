import { uiTest as test, expect } from './fixtures.js';

const TIMEOUT = 30_000;

test('shouldRejectOrderWithNonIntegerQuantity', async ({ config, shopPage }) => {
    const shopUiUrl = config.shop.frontendUrl;

    // When: place order with invalid quantity via UI
    await shopPage.goto(shopUiUrl);
    await shopPage.locator("a[href='/new-order']").click({ timeout: TIMEOUT });
    await shopPage.locator('[aria-label="SKU"]').fill('SOME-SKU', { timeout: TIMEOUT });
    await shopPage.locator('[aria-label="Quantity"]').fill('invalid-quantity', { timeout: TIMEOUT });
    await shopPage.locator('[aria-label="Country"]').fill('US', { timeout: TIMEOUT });
    await shopPage.locator('[aria-label="Place Order"]').click({ timeout: TIMEOUT });

    // Then: should see error notification
    const errorNotification = shopPage.locator("[role='alert'].notification.error");
    await errorNotification.waitFor({ state: 'visible', timeout: TIMEOUT });
    const errorMessage = await errorNotification.locator('.error-message').textContent({ timeout: TIMEOUT });
    expect(errorMessage?.trim()).toContain('The request contains one or more validation errors');
    const fieldErrors = await errorNotification.locator('.field-error').allTextContents();
    const quantityError = fieldErrors.find((text) => text.includes('quantity'));
    expect(quantityError).toContain('Quantity must be an integer');
});
