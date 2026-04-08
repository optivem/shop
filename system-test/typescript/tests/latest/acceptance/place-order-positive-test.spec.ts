import { test, forChannels } from './base/fixtures.js';
import { OrderStatus } from '../../../src/common/dtos.js';

forChannels('ui', 'api')(() => {
    test('shouldBeAbleToPlaceOrderForValidInput', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .then()
            .shouldSucceed();
    });

    test('orderPrefixShouldBeORD', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasOrderNumberPrefix('ORD-');
    });

    test('orderStatusShouldBePlacedAfterPlacingOrder', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasStatus(OrderStatus.PLACED);
    });

    test('shouldCalculateBasePriceAsProductOfUnitPriceAndQuantity', async ({ scenario }) => {
        await scenario
            .given()
            .product()
            .withUnitPrice(20)
            .when()
            .placeOrder()
            .withQuantity(5)
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasBasePrice(100);
    });

    const basePriceCases = [
        { unitPrice: '20.00', quantity: '5', basePrice: 100 },
        { unitPrice: '10.00', quantity: '3', basePrice: 30 },
        { unitPrice: '15.50', quantity: '4', basePrice: 62 },
        { unitPrice: '99.99', quantity: '1', basePrice: 99.99 },
    ];

    test.each(basePriceCases)(
        'shouldPlaceOrderWithCorrectBasePriceParameterized_unitPrice=$unitPrice_quantity=$quantity',
        async ({ scenario, unitPrice, quantity, basePrice }) => {
            await scenario
                .given()
                .product()
                .withUnitPrice(unitPrice)
                .when()
                .placeOrder()
                .withQuantity(quantity)
                .then()
                .shouldSucceed()
                .and()
                .order()
                .hasBasePrice(basePrice);
        },
    );

    test('discountRateShouldBeAppliedForCoupon', async ({ scenario }) => {
        const code = 'SUMMER2025';
        await scenario
            .given()
            .coupon()
            .withCouponCode(code)
            .withDiscountRate(0.15)
            .when()
            .placeOrder()
            .withCouponCode(code)
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasAppliedCouponCode(code)
            .hasDiscountRate(0.15);
    });

    test('discountRateShouldNotBeAppliedWhenThereIsNoCoupon', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasAppliedCouponCode(null)
            .hasDiscountRate(0)
            .hasDiscountAmount(0);
    });

    test('subtotalPriceShouldBeCalculatedAsTheBasePriceMinusDiscountAmountWhenWeHaveCoupon', async ({ scenario }) => {
        const code = 'SUMMER2025';
        await scenario
            .given()
            .coupon()
            .withCouponCode(code)
            .withDiscountRate(0.15)
            .and()
            .product()
            .withUnitPrice(20)
            .when()
            .placeOrder()
            .withQuantity(5)
            .withCouponCode(code)
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasBasePrice(100)
            .hasDiscountAmount(15)
            .hasSubtotalPrice(85);
    });

    test('subtotalPriceShouldBeSameAsBasePriceWhenNoCoupon', async ({ scenario }) => {
        await scenario
            .given()
            .product()
            .withUnitPrice(20)
            .when()
            .placeOrder()
            .withQuantity(5)
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasBasePrice(100)
            .hasDiscountAmount(0)
            .hasSubtotalPrice(100);
    });

    const taxRateCases = [
        { country: 'UK', taxRate: '0.09' },
        { country: 'US', taxRate: '0.20' },
    ];

    test.each(taxRateCases)(
        'correctTaxRateShouldBeUsedBasedOnCountry_country=$country',
        async ({ scenario, country, taxRate }) => {
            await scenario
                .given()
                .country()
                .withCode(country)
                .withTaxRate(taxRate)
                .when()
                .placeOrder()
                .withCountry(country)
                .then()
                .shouldSucceed()
                .and()
                .order()
                .hasTaxRate(Number.parseFloat(taxRate));
        },
    );

    const totalPriceCases = [
        { country: 'UK', taxRate: '0.09', subtotalPrice: 50, expectedTaxAmount: 4.5, expectedTotalPrice: 54.5 },
        { country: 'US', taxRate: '0.20', subtotalPrice: 100, expectedTaxAmount: 20, expectedTotalPrice: 120 },
    ];

    test.each(totalPriceCases)(
        'totalPriceShouldBeSubtotalPricePlusTaxAmount_country=$country',
        async ({ scenario, country, taxRate, subtotalPrice, expectedTaxAmount, expectedTotalPrice }) => {
            const unitPrice = subtotalPrice;
            await scenario
                .given()
                .product()
                .withUnitPrice(unitPrice)
                .and()
                .country()
                .withCode(country)
                .withTaxRate(taxRate)
                .when()
                .placeOrder()
                .withQuantity(1)
                .withCountry(country)
                .then()
                .shouldSucceed()
                .and()
                .order()
                .hasSubtotalPrice(subtotalPrice)
                .hasTaxAmount(expectedTaxAmount)
                .hasTotalPrice(expectedTotalPrice);
        },
    );

    test('couponUsageCountHasBeenIncrementedAfterItsBeenUsed', async ({ scenario }) => {
        const code = 'SUMMER2025';
        await scenario
            .given()
            .coupon()
            .withCouponCode(code)
            .withDiscountRate(0.1)
            .when()
            .placeOrder()
            .withCouponCode(code)
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasAppliedCouponCode(code);
    });
});

forChannels('api')(() => {
    test('orderTotalShouldIncludeTax', async ({ scenario }) => {
        await scenario
            .given()
            .country()
            .withCode('DE')
            .withTaxRate('0.19')
            .when()
            .placeOrder()
            .withCountry('DE')
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasSubtotalPrice(20)
            .hasTaxRate(0.19)
            .hasTotalPrice(23.8);
    });

    test('orderTotalShouldReflectCouponDiscount', async ({ scenario }) => {
        const couponCode = 'DISC10';
        await scenario
            .given()
            .coupon()
            .withCouponCode(couponCode)
            .withDiscountRate(0.1)
            .when()
            .placeOrder()
            .withCouponCode(couponCode)
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasSubtotalPrice(18)
            .hasDiscountRate(0.1)
            .hasAppliedCouponCode(couponCode)
            .hasTotalPrice(19.26);
    });

    test('orderTotalShouldApplyCouponDiscountAndTax', async ({ scenario }) => {
        const comboCode = 'COMBO10';
        await scenario
            .given()
            .coupon()
            .withCouponCode(comboCode)
            .withDiscountRate(0.1)
            .and()
            .country()
            .withCode('GB')
            .withTaxRate('0.20')
            .when()
            .placeOrder()
            .withCountry('GB')
            .withCouponCode(comboCode)
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasSubtotalPrice(18)
            .hasDiscountRate(0.1)
            .hasTaxRate(0.2)
            .hasAppliedCouponCode(comboCode)
            .hasTotalPrice(21.6);
    });
});
