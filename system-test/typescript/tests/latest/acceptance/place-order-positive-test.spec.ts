import { test, forChannels, ChannelType } from './base/fixtures.js';
import { OrderStatus } from '../../../src/testkit/common/dtos.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToPlaceOrderForValidInput', async ({ scenario }) => {
        await scenario
            .given()
            .product()
            .withSku('ABC')
            .withUnitPrice(20)
            .and()
            .country()
            .withCode('US')
            .withTaxRate(0.1)
            .when()
            .placeOrder()
            .withSku('ABC')
            .withQuantity(5)
            .withCountry('US')
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
            .hasBasePrice('100.00');
    });

    const basePriceCases = [
        { unitPrice: '20.00', quantity: '5', basePrice: '100.00' },
        { unitPrice: '10.00', quantity: '3', basePrice: '30.00' },
        { unitPrice: '15.50', quantity: '4', basePrice: '62.00' },
        { unitPrice: '99.99', quantity: '1', basePrice: '99.99' },
    ];

    test.eachAlsoFirstRow(basePriceCases)(
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
            .hasAppliedCoupon(code)
            .hasDiscountRate(0.15);
    });

    test('discountRateShouldBeNotAppliedWhenThereIsNoCoupon', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withCouponCode(null)
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasAppliedCoupon(null)
            .hasDiscountRate(0)
            .hasDiscountAmount('0.00');
    });

    test('subtotalPriceShouldBeCalculatedAsTheBasePriceMinusDiscountAmountWhenWeHaveCoupon', async ({ scenario }) => {
        await scenario
            .given()
            .coupon()
            .withDiscountRate(0.15)
            .and()
            .product()
            .withUnitPrice(20)
            .when()
            .placeOrder()
            .withCouponCode()
            .withQuantity(5)
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasAppliedCoupon()
            .hasDiscountRate(0.15)
            .hasBasePrice('100.00')
            .hasDiscountAmount('15.00')
            .hasSubtotalPrice('85.00');
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
            .hasBasePrice('100.00')
            .hasDiscountAmount('0.00')
            .hasSubtotalPrice('100.00');
    });

    const taxRateCases = [
        { country: 'UK', taxRate: '0.09' },
        { country: 'US', taxRate: '0.20' },
    ];

    test.eachAlsoFirstRow(taxRateCases)(
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
        { country: 'UK', taxRate: '0.09', unitPrice: '50.00', subtotalPrice: '50.00', expectedTaxAmount: '4.50', expectedTotalPrice: '54.50' },
        { country: 'US', taxRate: '0.20', unitPrice: '100.00', subtotalPrice: '100.00', expectedTaxAmount: '20.00', expectedTotalPrice: '120.00' },
    ];

    test.eachAlsoFirstRow(totalPriceCases)(
        'totalPriceShouldBeSubtotalPricePlusTaxAmount_country=$country',
        async ({ scenario, country, taxRate, unitPrice, subtotalPrice, expectedTaxAmount, expectedTotalPrice }) => {
            await scenario
                .given()
                .country()
                .withCode(country)
                .withTaxRate(taxRate)
                .and()
                .product()
                .withUnitPrice(unitPrice)
                .when()
                .placeOrder()
                .withCountry(country)
                .withQuantity(1)
                .then()
                .shouldSucceed()
                .and()
                .order()
                .hasTaxRate(Number.parseFloat(taxRate))
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
            .when()
            .placeOrder()
            .withCouponCode(code)
            .then()
            .shouldSucceed()
            .and()
            .coupon(code)
            .hasUsedCount(1);
    });
});

forChannels(ChannelType.API)(() => {
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
            .hasSubtotalPrice('20.00')
            .hasTaxRate(0.19)
            .hasTotalPrice('23.80');
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
            .hasSubtotalPrice('18.00')
            .hasDiscountRate(0.1)
            .hasAppliedCoupon(couponCode)
            .hasTotalPrice('19.26');
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
            .hasSubtotalPrice('18.00')
            .hasDiscountRate(0.1)
            .hasTaxRate(0.2)
            .hasAppliedCoupon(comboCode)
            .hasTotalPrice('21.60');
    });
});
