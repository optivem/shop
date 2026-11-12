package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseCheckOutTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

// The cart lives in the browser and only the UI checks it out; there is no checkout API. The orders it places are
// checked through the order API.
@Isolated("stages the shared ERP stub's product list and saves the shared test customer's profile")
class CheckOutPositiveIsolatedTest extends BaseCheckOutTest {
    private static final String SUMMER2025 = "SUMMER2025";

    @Tag("UC-009.1")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldPlaceOneOrderPerCartLine() {
        var address = ANA_NOVAK_BERLIN;
        givenTheCatalogAndSavedAddress(address)
                .when().checkOut()
                    .withCartContaining(HP_15, 2)
                    .withCartContaining(DELL_XPS, 1)
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .checkout()
                        .confirmsOrdersFor(HP_15, DELL_XPS)
                    .and().orderFor(HP_15)
                        .hasQuantity(2)
                        .hasShippingAddressWithCountryCode(address.firstName(), address.lastName(), address.street(),
                                address.postalCode(), address.city(), address.country())
                    .and().orderFor(DELL_XPS)
                        .hasQuantity(1)
                        .hasShippingAddressWithCountryCode(address.firstName(), address.lastName(), address.street(),
                                address.postalCode(), address.city(), address.country());
    }

    // The confirmation lists the orders in the order they were placed.
    @Tag("UC-009.1")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldPlaceTheOrdersInCartOrder() {
        givenTheCatalogAndSavedAddress(ANA_NOVAK_BERLIN)
                .when().checkOut()
                    .withCartContaining(LOGI_MX3S, 1)
                    .withCartContaining(HP_15, 1)
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .checkout()
                        .confirmsOrdersFor(LOGI_MX3S, HP_15);
    }

    private static Stream<Arguments> provideTaxCases() {
        return Stream.of(
                Arguments.of(JOHN_SMITH_NEW_YORK, "US", "0.00"),
                Arguments.of(ANA_NOVAK_BERLIN, "DE", "0.19"),
                Arguments.of(JANE_BROWN_LONDON, "GB", "0.20")
        );
    }

    @Tag("UC-009.2")
    @TestTemplate
    @Channel(ChannelType.UI)
    @MethodSource("provideTaxCases")
    void shouldApplyTheTaxRateOfTheShippingAddressesCountry(Address address, String country, String taxRate) {
        givenTheCatalogAndSavedAddress(address)
                .when().checkOut()
                    .withCartContaining(LOGI_MX3S, 1)
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .orderFor(LOGI_MX3S)
                        .hasCountryCode(country)
                        .hasTaxRate(taxRate);
    }

    @Tag("UC-009.2")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShowTheEstimatedTaxForTheChosenShippingCountry() {
        var other = JANE_BROWN_LONDON;
        givenTheCatalogAndSavedAddress(ANA_NOVAK_BERLIN)
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                    .withShippingAddress(other.firstName(), other.lastName(), other.street(), other.postalCode(),
                            other.city(), other.country())
                .then().shouldSucceed()
                    .checkout()
                        .hadTaxWhenOpened("DE", "19%")
                        .hasTax("GB", "20%");
    }

    @Tag("UC-009.3")
    @Tag("UC-010.5")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldPreselectTheSavedShippingAddress() {
        givenTheCatalogAndSavedAddress(ANA_NOVAK_BERLIN)
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                .then().shouldSucceed()
                    .checkout()
                        .hasSavedAddressSelected(ANA_NOVAK_BERLIN.formatted());
    }

    @Tag("UC-009.3")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldShipToADifferentAddressWithoutChangingTheProfile() {
        var saved = ANA_NOVAK_BERLIN;
        var other = JANE_BROWN_LONDON;
        givenTheCatalogAndSavedAddress(saved)
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                    .withShippingAddress(other.firstName(), other.lastName(), other.street(), other.postalCode(),
                            other.city(), other.country())
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .orderFor(HP_15)
                        .hasShippingAddressWithCountryCode(other.firstName(), other.lastName(), other.street(),
                                other.postalCode(), other.city(), other.country())
                        .hasTaxRate("0.20")
                    .and().profile()
                        .hasFirstName(saved.firstName())
                        .hasLastName(saved.lastName())
                        .hasStreet(saved.street())
                        .hasPostalCode(saved.postalCode())
                        .hasCity(saved.city())
                        .hasCountry(saved.country());
    }

    @Tag("UC-009.4")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldApplyTheCouponToEveryOrder() {
        givenTheCatalogAndSavedAddress(JOHN_SMITH_NEW_YORK)
                .coupon()
                    .withCouponCode(SUMMER2025)
                    .withDiscountRate("0.15")
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                    .withCartContaining(LOGI_MX3S, 1)
                    .withCouponCode(SUMMER2025)
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .checkout()
                        .confirmsOrdersFor(HP_15, LOGI_MX3S)
                    .and().orderFor(HP_15)
                        .hasAppliedCoupon(SUMMER2025)
                        .hasDiscountRate("0.15")
                    .and().orderFor(LOGI_MX3S)
                        .hasAppliedCoupon(SUMMER2025)
                        .hasDiscountRate("0.15")
                    .and().coupon(SUMMER2025)
                        .hasUsedCount(2);
    }

    @Tag("UC-009.4")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldApplyNoDiscountWhenThereIsNoCoupon() {
        givenTheCatalogAndSavedAddress(JOHN_SMITH_NEW_YORK)
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .orderFor(HP_15)
                        .hasAppliedCoupon(null)
                        .hasDiscountAmount("0.00");
    }

    @Tag("UC-009.5")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldReviewTheCartLinesAndSubtotalAtCatalogPrices() {
        givenTheCatalogAndSavedAddress(ANA_NOVAK_BERLIN)
                .when().checkOut()
                    .withCartContaining(HP_15, 2)
                    .withCartContaining(DELL_XPS, 1)
                .then().shouldSucceed()
                    .checkout()
                        .hasReviewLines(HP_15, DELL_XPS)
                        .hasReviewLine(HP_15, 2, "$1,399.98")
                        .hasReviewLine(DELL_XPS, 1, "$1,299.99")
                        .hasReviewSubtotal("$2,699.97")
                        .saysDiscountAndTaxAreWorkedOutPerOrder();
    }

    @Tag("UC-009.5")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldPriceEachOrderFromTheErpWhenItIsPlaced() {
        givenTheCatalogAndSavedAddress(JOHN_SMITH_NEW_YORK)
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                    .withErpPriceChanged(HP_15, "649.99")
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .orderFor(HP_15)
                        .hasUnitPrice("649.99");
    }

    @Tag("UC-009.6")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldConfirmEveryOrderAndEmptyTheCart() {
        givenTheCatalogAndSavedAddress(ANA_NOVAK_BERLIN)
                .when().checkOut()
                    .withCartContaining(HP_15, 2)
                    .withCartContaining(DELL_XPS, 1)
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .checkout()
                        .thanksForTheOrder()
                        .confirmsOrdersFor(HP_15, DELL_XPS)
                        .confirmsNothingRejected()
                        .linksEachOrderToItsDetails()
                        .offersViewMyOrdersAndContinueShopping()
                        .hasCartLines();
    }
}
