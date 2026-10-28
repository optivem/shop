import { test, forChannels, ChannelType } from './base/fixtures.js';
import { OrderStatus } from '../../../src/testkit/common/dtos.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToViewOrder', async ({ scenario }) => {
        await scenario
            .given()
            .product()
            .withSku('BOOK-123')
            .withUnitPrice(20)
            .and()
            .country()
            .withCode('US')
            .withTaxRate('0.10')
            .and()
            .coupon()
            .withCouponCode('SUMMER2025')
            .withDiscountRate(0.1)
            .and()
            .order()
            .withSku('BOOK-123')
            .withQuantity(5)
            .withCountry('US')
            .withCouponCode('SUMMER2025')
            .when()
            .viewOrder()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasOrderNumberPrefix('ORD-')
            .hasSku('BOOK-123')
            .hasQuantity(5)
            .hasCountry('US')
            .hasUnitPrice(20)
            .hasBasePrice(100)
            .hasAppliedCoupon('SUMMER2025')
            .hasDiscountRate(0.1)
            .hasDiscountAmount(10)
            .hasSubtotalPrice(90)
            .hasTaxRate(0.1)
            .hasTaxAmount(9)
            .hasTotalPrice(99)
            .hasStatus(OrderStatus.PLACED);
    });

    test('customerShouldBeAbleToViewOwnOrder', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsCustomer()
            .when()
            .viewOrder()
            .then()
            .shouldSucceed();
    });

    test('sameCustomerAliasShouldResolveToSameCustomer', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer('B')
            .and()
            .loggedInAsCustomer('B')
            .when()
            .viewOrder()
            .then()
            .shouldSucceed();
    });

    test('adminShouldBeAbleToViewCustomerOrder', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsAdmin()
            .when()
            .viewOrder()
            .then()
            .shouldSucceed();
    });

    test('adminShouldBeAbleToViewAnotherCustomersOrder', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer('B')
            .and()
            .loggedInAsAdmin()
            .when()
            .viewOrder()
            .then()
            .shouldSucceed();
    });
});
