import { MatchersV3 } from '@pact-foundation/pact';
import type { V3Interaction } from '@pact-foundation/pact';

const { like, eachLike, integer, regex } = MatchersV3;

// Rates travel as decimal strings with a fixed scale of 4dp.
const RATE = String.raw`^-?\d+\.\d{4}$`;

export function browseCouponsInteraction(): V3Interaction {
  return {
    states: [{ description: 'at least one coupon exists' }],
    uponReceiving: 'a browse-coupons request',
    withRequest: { method: 'GET', path: '/api/coupons' },
    willRespondWith: {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
      body: {
        coupons: eachLike({
          code: like('SAVE10'),
          discountRate: regex(RATE, '0.2000'),
          usageLimit: integer(100),
          usedCount: integer(0),
        }),
      },
    },
  };
}

interface PublishCouponParams {
  code: string;
  discountRate: string;
}

export function publishCouponInteraction({ code, discountRate }: PublishCouponParams): V3Interaction {
  return {
    states: [{ description: `no coupon ${code} exists yet` }],
    uponReceiving: `a publish-coupon request for ${code}`,
    withRequest: {
      method: 'POST',
      path: '/api/coupons',
      headers: { 'Content-Type': 'application/json' },
      body: { code, discountRate },
    },
    willRespondWith: { status: 204 },
  };
}
