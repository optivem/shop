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

import static com.mycompany.myshop.systemtest.commons.constants.ErrorMessages.AUTHENTICATION_REQUIRED;
import static com.mycompany.myshop.systemtest.commons.constants.ErrorMessages.PERMISSION_DENIED;

// The cart lives in the browser and only the UI checks it out; there is no checkout API.
//
// The @UC-009.10 scenarios (first order) sign in as a new customer, who has no saved profile. The @UC-009.11 scenarios
// start from a saved address and choose "Ship to a different address": the same form and the same rules as on a
// first order.
@Isolated("stages the shared ERP stub's product list and saves the shared test customer's profile")
class CheckOutNegativeIsolatedTest extends BaseCheckOutTest {
    private static final String PLEASE_CORRECT_THE_ADDRESS = "Please correct the shipping address.";
    private static final String NO_ORDER_WAS_PLACED = "No order was placed.";

    @Tag("UC-009.7")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldAskAnAnonymousVisitorToSignInBeforeCheckout() {
        givenTheCatalog()
                .and().notLoggedIn()
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                    .withProceededFromCart()
                .then().shouldFail()
                    .errorMessage(AUTHENTICATION_REQUIRED);
    }

    @Tag("UC-009.7")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldReturnToTheCheckoutWithTheCartUnchangedAfterSignIn() {
        givenTheCatalog()
                .and().notLoggedIn()
                .when().checkOut()
                    .withCartContaining(HP_15, 2)
                    .withCartContaining(LOGI_MX3S, 1)
                    .withProceededFromCartAndSignedIn()
                .then().shouldSucceed()
                    .checkout()
                        .isShown()
                        .hasReviewLines(HP_15, LOGI_MX3S)
                        .hasReviewQuantity(HP_15, 2)
                        .hasReviewQuantity(LOGI_MX3S, 1);
    }

    @Tag("UC-009.8")
    @TestTemplate
    @Channel(ChannelType.UI)
    void adminShouldNotBeAbleToCheckOut() {
        givenTheCatalog()
                .and().loggedInAsAdmin()
                .when().checkOut()
                .then().shouldFail()
                    .errorMessage(PERMISSION_DENIED);
    }

    @Tag("UC-009.9")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldSendTheCustomerToTheCartWhenTheCartIsEmpty() {
        givenTheCatalog()
                .and().loggedInAsCustomer()
                .when().checkOut()
                .then().shouldSucceed()
                    .checkout()
                        .showsTheCart()
                        .showsMessage("Your cart is empty.");
    }

    @Tag("UC-009.10")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldOpenTheAddressFormOnAFirstOrder() {
        givenTheCatalog()
                .and().newCustomer()
                    .withFirstName("Ana")
                    .withLastName("Novak")
                .and().loggedInAsCustomer()
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                .then().shouldSucceed()
                    .checkout()
                        .hasAddressFormOpen()
                        .hasShippingName("Ana", "Novak")
                        .hasSaveAsDefaultTicked();
    }

    @Tag("UC-009.10")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldSaveTheAddressToTheProfileWhenSaveAsDefaultIsTicked() {
        var address = ANA_NOVAK_BERLIN;
        givenTheCatalog()
                .and().newCustomer()
                .and().loggedInAsCustomer()
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                    .withShippingAddress(address.firstName(), address.lastName(), address.street(),
                            address.postalCode(), address.city(), address.country())
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .checkout()
                        .confirmsOrdersFor(HP_15)
                    .and().profile()
                        .hasFirstName(address.firstName())
                        .hasLastName(address.lastName())
                        .hasStreet(address.street())
                        .hasPostalCode(address.postalCode())
                        .hasCity(address.city())
                        .hasCountry(address.country());
    }

    @Tag("UC-009.10")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldNotSaveTheAddressWhenSaveAsDefaultIsUnticked() {
        var address = ANA_NOVAK_BERLIN;
        givenTheCatalog()
                .and().newCustomer()
                .and().loggedInAsCustomer()
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                    .withShippingAddress(address.firstName(), address.lastName(), address.street(),
                            address.postalCode(), address.city(), address.country())
                    .withSaveAsDefaultUnticked()
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .checkout()
                        .confirmsOrdersFor(HP_15)
                    .and().profile()
                        .hasNoSavedAddress();
    }

    private static Stream<Arguments> provideMissingFieldCases() {
        return Stream.of(
                Arguments.of("first name", "First name is required"),
                Arguments.of("last name", "Last name is required"),
                Arguments.of("street", "Street is required"),
                Arguments.of("postal code", "Postal code is required"),
                Arguments.of("city", "City is required"),
                Arguments.of("country", "Country is required")
        );
    }

    @Tag("UC-009.11")
    @TestTemplate
    @Channel(ChannelType.UI)
    @MethodSource("provideMissingFieldCases")
    void shouldRejectAnIncompleteShippingAddress(String field, String expectedError) {
        var address = JANE_BROWN_LONDON.withEmpty(field);
        givenTheCatalogAndSavedAddress(ANA_NOVAK_BERLIN)
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                    .withShippingAddress(address.firstName(), address.lastName(), address.street(),
                            address.postalCode(), address.city(), address.country())
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .checkout()
                        .isShown()
                        .confirmsNoOrder()
                        .showsMessage(PLEASE_CORRECT_THE_ADDRESS)
                        .showsMessageDetail(expectedError)
                        .hasCartLines(HP_15)
                        .hasCartLineQuantity(HP_15, 1);
    }

    // The country select offers only US, DE and GB, so FR is sent the way an edited form would send it.
    @Tag("UC-009.11")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldRejectAShippingCountryThatIsNotSupported() {
        var address = MARIE_DUPONT_PARIS;
        givenTheCatalogAndSavedAddress(ANA_NOVAK_BERLIN)
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                    .withShippingAddress(address.firstName(), address.lastName(), address.street(),
                            address.postalCode(), address.city(), address.country())
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .checkout()
                        .isShown()
                        .confirmsNoOrder()
                        .showsMessage(PLEASE_CORRECT_THE_ADDRESS)
                        .showsMessageDetail("Country must be US, DE or GB");
    }

    @Tag("UC-009.12")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldStillPlaceTheOtherLinesWhenTheCouponRunsOutPartWay() {
        givenTheCatalogAndSavedAddress(ANA_NOVAK_BERLIN)
                .coupon()
                    .withCouponCode("LIMITED2024")
                    .withUsageLimit(1)
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                    .withCartContaining(DELL_XPS, 1)
                    .withCouponCode("LIMITED2024")
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .checkout()
                        .confirmsOrdersFor(HP_15)
                        .confirmsRejected(DELL_XPS, "Coupon code LIMITED2024 has exceeded its usage limit")
                        .hasCartLines(DELL_XPS)
                        .hasCartLineQuantity(DELL_XPS, 1)
                    .and().orderFor(HP_15)
                        .hasAppliedCoupon("LIMITED2024");
    }

    @Tag("UC-009.12")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldStillPlaceTheOtherLinesWhenAProductIsNoLongerOffered() {
        givenTheCatalogAndSavedAddress(ANA_NOVAK_BERLIN)
                .when().checkOut()
                    .withCartContaining(DELL_XPS, 1)
                    .withCartContaining(LOGI_MX3S, 2)
                    .withErpNoLongerOffering(DELL_XPS)
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .checkout()
                        .confirmsOrdersFor(LOGI_MX3S)
                        .confirmsRejected(DELL_XPS, "Product does not exist for SKU: DELL-XPS")
                        .hasCartLines(DELL_XPS)
                        .hasCartLineQuantity(DELL_XPS, 1);
    }

    @Tag("UC-009.13")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldPlaceNothingAndKeepTheCartWhenEveryLineIsRejected() {
        givenTheCatalogAndSavedAddress(ANA_NOVAK_BERLIN)
                .when().checkOut()
                    .withCartContaining(HP_15, 1)
                    .withCartContaining(DELL_XPS, 1)
                    .withCouponCode("INVALIDCOUPON")
                    .withOrderPlaced()
                .then().shouldSucceed()
                    .checkout()
                        .isShown()
                        .confirmsNoOrder()
                        .showsMessage(NO_ORDER_WAS_PLACED)
                        .saysRejected(HP_15, "Coupon code INVALIDCOUPON does not exist")
                        .saysRejected(DELL_XPS, "Coupon code INVALIDCOUPON does not exist")
                        .hasCartLines(HP_15, DELL_XPS);
    }
}
