package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

class ViewOrderPositiveTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldBeAbleToViewOrder() {
        scenario
                .given().product()
                    .withSku("BOOK-123")
                    .withUnitPrice(20.00)
                .and().country()
                    .withCode("US")
                    .withTaxRate("0.10")
                .and().coupon()
                    .withCouponCode("SUMMER2025")
                    .withDiscountRate(0.10)
                .and().order()
                    .withSku("BOOK-123")
                    .withQuantity(5)
                    .withCountry("US")
                    .withCouponCode("SUMMER2025")
                .when().viewOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasOrderNumberPrefix("ORD-")
                    .hasSku("BOOK-123")
                    .hasQuantity(5)
                    .hasCountry("US")
                    .hasUnitPrice(20.00)
                    .hasAppliedCoupon("SUMMER2025")
                    .hasDiscountRate(0.10)
                    .hasTaxRate(0.10)
                    .hasStatus(OrderStatus.PLACED);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void customerShouldBeAbleToViewOwnOrder() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsCustomer()
                .when().viewOrder()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void sameCustomerAliasShouldResolveToSameCustomer() {
        scenario
                .given().order()
                    .placedByCustomer("B")
                .and().loggedInAsCustomer("B")
                .when().viewOrder()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldBeAbleToViewCustomerOrder() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsAdmin()
                .when().viewOrder()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldBeAbleToViewAnotherCustomersOrder() {
        scenario
                .given().order()
                    .placedByCustomer("B")
                .and().loggedInAsAdmin()
                .when().viewOrder()
                .then().shouldSucceed();
    }
}
