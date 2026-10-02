import { uiTest as test, expect } from './base/BaseE2eTest.js';
import { loginToMyShopUiIfRequired } from '../../../../src/testkit/driver/adapter/shared/client/playwright/keycloak-ui-login.js';

const TIMEOUT = 30_000;

test('shouldRejectOrderWithNonIntegerQuantity', async ({ config, myShopPage }) => {
    const myShopUiUrl = config.myShop.frontendUrl;

    // When: place order with invalid quantity via UI
    await myShopPage.goto(myShopUiUrl);
    await loginToMyShopUiIfRequired(myShopPage, config.keycloakUrl);
    await myShopPage.locator("a[href='/new-order']").click({ timeout: TIMEOUT });
    await myShopPage.locator('[aria-label="SKU"]').fill('SOME-SKU', { timeout: TIMEOUT });
    await myShopPage.locator('[aria-label="Quantity"]').fill('invalid-quantity', { timeout: TIMEOUT });
    await myShopPage.locator('[aria-label="Country"]').fill('US', { timeout: TIMEOUT });
    await myShopPage.locator('[aria-label="Place Order"]').click({ timeout: TIMEOUT });

    // Then: should see an error alert whose text contains the validation message,
    // the "quantity" field, and the integer constraint message (single text match,
    // matching Java/.NET test shape).
    const errorAlert = myShopPage.locator("[role='alert'][data-notification-id]");
    await errorAlert.waitFor({ state: 'visible', timeout: TIMEOUT });
    const errorText = (await errorAlert.textContent({ timeout: TIMEOUT })) ?? '';
    expect(errorText).toContain('The request contains one or more validation errors');
    expect(errorText).toContain('quantity');
    expect(errorText).toContain('Quantity must be an integer');
});
