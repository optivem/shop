import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';

const ERP_UNAVAILABLE = "Product information can't be retrieved right now";
// A down ERP answers at once, so only the retry backoff (well under a second) delays the 503.
const ERP_DOWN_FAIL_FAST_SECONDS = 5;
// A slow ERP costs up to 3 attempts x 2s timeout plus backoff; the stubbed ERP would take 20s.
const ERP_SLOW_FAIL_FAST_SECONDS = 12;

test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });
    forChannels(ChannelType.UI, ChannelType.API)(() => {
        test('cannotPlaceOrderWithExpiredCoupon', criteria('UC-001.11'), async ({ scenario }) => {
            await scenario
                .given()
                .clock()
                .withTime('2023-09-01T12:00:00Z')
                .and()
                .coupon()
                .withCouponCode('SUMMER2023')
                .withValidFrom('2023-06-01T00:00:00Z')
                .withValidTo('2023-08-31T23:59:59Z')
                .when()
                .placeOrder()
                .withCouponCode('SUMMER2023')
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('couponCode', 'Coupon code SUMMER2023 has expired');
        });

        test('shouldRejectOrderPlacedAtYearEnd', criteria('UC-001.13'), async ({ scenario }) => {
            await scenario
                .given()
                .clock()
                .withTime('2026-12-31T23:59:30Z')
                .when()
                .placeOrder()
                .then()
                .shouldFail()
                .errorMessage('Orders cannot be placed between 23:59 and 00:00 on December 31st');
        });

        test('shouldRejectOrderWhenErpIsUnavailable', criteria('UC-001.14'), async ({ scenario }) => {
            await scenario
                .given()
                .erp()
                .unavailable()
                .when()
                .placeOrder()
                .then()
                .shouldFail()
                .errorMessage(ERP_UNAVAILABLE);
        });
    });

    // Resilience: the system fails fast with 503 + Retry-After instead of hanging or guessing a price.
    // API only, because the assertions are about the HTTP response.
    forChannels(ChannelType.API)(() => {
        test('shouldFailFastWithServiceUnavailableWhenErpIsDown', criteria('UC-001.14'), async ({ scenario }) => {
            await scenario
                .given()
                .erp()
                .unavailable()
                .when()
                .placeOrder()
                .then()
                .shouldFail()
                .errorMessage(ERP_UNAVAILABLE)
                .serviceUnavailable()
                .failedWithinSeconds(ERP_DOWN_FAIL_FAST_SECONDS);
        });

        // The slow ERP answers only after 20s, far past the 2s outbound timeout: the call must give up on its
        // own (timeout times retries) well before the ERP would have answered.
        test('shouldFailFastWithServiceUnavailableWhenErpIsSlow', criteria('UC-001.14'), async ({ scenario }) => {
            await scenario
                .given()
                .erp()
                .slow()
                .when()
                .placeOrder()
                .then()
                .shouldFail()
                .errorMessage(ERP_UNAVAILABLE)
                .serviceUnavailable()
                .failedWithinSeconds(ERP_SLOW_FAIL_FAST_SECONDS);
        });
    });
});
