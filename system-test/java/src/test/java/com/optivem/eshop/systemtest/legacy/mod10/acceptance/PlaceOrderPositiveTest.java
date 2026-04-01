package com.optivem.eshop.systemtest.legacy.mod10.acceptance;

import com.optivem.eshop.systemtest.legacy.mod10.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.testing.Channel;
import com.optivem.testing.DataSource;

import org.junit.jupiter.api.TestTemplate;

class PlaceOrderPositiveTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldBeAbleToPlaceOrderForValidInput() {
        scenario
                .given().product()
                    .withSku("ABC")
                    .withUnitPrice(20.00)
                .and().country()
                    .withCode("US")
                    .withTaxRate(0.10)
                .when().placeOrder()
                    .withSku("ABC")
                    .withQuantity(5)
                    .withCountry("US")
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void orderStatusShouldBePlacedAfterPlacingOrder() {
        scenario
                .when().placeOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasStatus(OrderStatus.PLACED);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldCalculateBasePriceAsProductOfUnitPriceAndQuantity() {
        scenario
                .given().product()
                    .withUnitPrice(20.00)
                .when().placeOrder()
                    .withQuantity(5)
                .then().shouldSucceed()
                .and().order()
                    .hasBasePrice(100.00);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @DataSource({"20.00", "5", "100.00"})
    @DataSource({"10.00", "3", "30.00"})
    @DataSource({"15.50", "4", "62.00"})
    @DataSource({"99.99", "1", "99.99"})
    void shouldPlaceOrderWithCorrectBasePriceParameterized(String unitPrice, String quantity, String basePrice) {
        scenario
                .given().product()
                    .withUnitPrice(unitPrice)
                .when().placeOrder()
                    .withQuantity(quantity)
                .then().shouldSucceed()
                .and().order()
                    .hasBasePrice(basePrice);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void orderPrefixShouldBeORD() {
        scenario
                .when().placeOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasOrderNumberPrefix("ORD-");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void discountRateShouldBeAppliedForCoupon() {
        scenario
                .given().coupon()
                    .withCouponCode("SUMMER2025")
                    .withDiscountRate(0.15)
                .when().placeOrder()
                    .withCouponCode("SUMMER2025")
                .then().shouldSucceed()
                .and().order()
                    .hasAppliedCoupon("SUMMER2025")
                    .hasDiscountRate(0.15);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void discountRateShouldBeNotAppliedWhenThereIsNoCoupon() {
        scenario
                .when().placeOrder()
                    .withCouponCode(null)
                .then().shouldSucceed()
                .and().order()
                    .hasAppliedCoupon(null)
                    .hasDiscountRate(0.00)
                    .hasDiscountAmount(0.00);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void subtotalPriceShouldBeCalculatedAsTheBasePriceMinusDiscountAmountWhenWeHaveCoupon() {
        scenario
                .given().coupon()
                    .withDiscountRate(0.15)
                .and().product()
                    .withUnitPrice(20.00)
                .when().placeOrder()
                    .withCouponCode()
                    .withQuantity(5)
                .then().shouldSucceed()
                .and().order()
                    .hasAppliedCoupon()
                    .hasDiscountRate(0.15)
                    .hasBasePrice(100.00)
                    .hasDiscountAmount(15.00)
                    .hasSubtotalPrice(85.00);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void subtotalPriceShouldBeSameAsBasePriceWhenNoCoupon() {
        scenario
                .given().product()
                    .withUnitPrice(20.00)
                .when().placeOrder()
                    .withQuantity(5)
                .then().shouldSucceed()
                .and().order()
                    .hasBasePrice(100.00)
                    .hasDiscountAmount(0.00)
                    .hasSubtotalPrice(100.00);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @DataSource({"UK", "0.09"})
    @DataSource({"US", "0.20"})
    void correctTaxRateShouldBeUsedBasedOnCountry(String country, String taxRate) {
        scenario
                .given().country()
                    .withCode(country)
                    .withTaxRate(taxRate)
                .when().placeOrder()
                    .withCountry(country)
                .then().shouldSucceed()
                .and().order()
                    .hasTaxRate(taxRate);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @DataSource({"UK", "0.09", "50.00", "4.50", "54.50"})
    @DataSource({"US", "0.20", "100.00", "20.00", "120.00"})
    void totalPriceShouldBeSubtotalPricePlusTaxAmount(String country, String taxRate, String subtotalPrice, String expectedTaxAmount, String expectedTotalPrice) {
        scenario
                .given().country()
                    .withCode(country)
                    .withTaxRate(taxRate)
                .and().product()
                    .withUnitPrice(subtotalPrice)
                .when().placeOrder()
                    .withCountry(country)
                    .withQuantity(1)
                .then().shouldSucceed()
                .and().order()
                    .hasTaxRate(taxRate)
                    .hasSubtotalPrice(subtotalPrice)
                    .hasTaxAmount(expectedTaxAmount)
                    .hasTotalPrice(expectedTotalPrice);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void couponUsageCountHasBeenIncrementedAfterItsBeenUsed() {
        scenario
                .given().coupon()
                    .withCouponCode("SUMMER2025")
                .when().placeOrder()
                    .withCouponCode("SUMMER2025")
                .then().shouldSucceed()
                .and().coupon("SUMMER2025")
                    .hasUsedCount(1);
    }

    // TODO: Place order for exact available quantity
}




