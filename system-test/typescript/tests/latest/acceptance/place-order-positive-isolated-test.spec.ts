import { test, forChannels } from './base/fixtures.js';

forChannels('ui', 'api')(() => {
    test('shouldRecordPlacementTimestamp', async ({ scenario }) => {
        await scenario
            .given()
            .clock()
            .withTime('2026-01-15T10:30:00Z')
            .when()
            .placeOrder()
            .then()
            .shouldSucceed()
            .and()
            .clock()
            .hasTime('2026-01-15T10:30:00Z');
    });

    test('shouldApplyFullPriceWithoutPromotion', async ({ scenario }) => {
        await scenario
            .given()
            .product()
            .withUnitPrice(20.0)
            .and()
            .promotion()
            .withActive(false)
            .and()
            .country()
            .withTaxRate('0.00')
            .when()
            .placeOrder()
            .withQuantity(5)
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasTotalPrice(100.0);
    });

    test('shouldApplyDiscountWhenPromotionIsActive', async ({ scenario }) => {
        await scenario
            .given()
            .product()
            .withUnitPrice(20.0)
            .and()
            .promotion()
            .withActive(true)
            .withDiscount(0.5)
            .and()
            .country()
            .withTaxRate('0.00')
            .when()
            .placeOrder()
            .withQuantity(5)
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasTotalPrice(50.0);
    });
});
