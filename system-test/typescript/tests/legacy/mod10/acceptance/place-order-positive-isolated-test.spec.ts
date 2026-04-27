import { test, forChannels, ChannelType } from './base/BaseAcceptanceTest.js';

test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });
    forChannels(ChannelType.UI, ChannelType.API)(() => {
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

        test('shouldApplyFullPriceOnWeekday', async ({ scenario }) => {
            await scenario
                .given()
                .product()
                .withUnitPrice(20)
                .and()
                .promotion()
                .withActive(false)
                .and()
                .country()
                .withTaxRate('0.00')
                .and()
                .clock()
                .withWeekday()
                .when()
                .placeOrder()
                .withQuantity(5)
                .then()
                .shouldSucceed()
                .and()
                .order()
                .hasTotalPrice('100.00');
        });

        test('shouldApplyDiscountWhenPromotionIsActive', async ({ scenario }) => {
            await scenario
                .given()
                .product()
                .withUnitPrice(20)
                .and()
                .promotion()
                .withActive(true)
                .withDiscount('0.5')
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
                .hasTotalPrice('50.00');
        });
    });
});
