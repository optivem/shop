import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';

test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });
    forChannels(ChannelType.UI, ChannelType.API)(() => {
        // The UI reads the coupon back from the coupon list, newest first with no paging: coupons published in
        // parallel would push it off the page.
        test('couponUsageCountHasBeenIncrementedAfterItsBeenUsed', criteria('UC-001.5', 'UC-005.4'), async ({ scenario }) => {
            const code = 'SUMMER2025';
            await scenario
                .given()
                .coupon()
                .withCouponCode(code)
                .when()
                .placeOrder()
                .withCouponCode(code)
                .then()
                .shouldSucceed()
                .and()
                .coupon(code)
                .hasUsedCount(1);
        });

        test('shouldRecordPlacementTimestamp', criteria('UC-001.6'), async ({ scenario }) => {
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

        test('shouldApplyFullPriceWithoutPromotion', criteria('UC-001.7'), async ({ scenario }) => {
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
                .when()
                .placeOrder()
                .withQuantity(5)
                .then()
                .shouldSucceed()
                .and()
                .order()
                .hasTotalPrice('100.00');
        });

        test('shouldApplyDiscountWhenPromotionIsActive', criteria('UC-001.7'), async ({ scenario }) => {
            await scenario
                .given()
                .product()
                .withUnitPrice(20)
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
                .hasTotalPrice('50.00');
        });
    });
});
