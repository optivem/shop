import { MatchersV3 } from '@pact-foundation/pact';
import type { V3Interaction } from '@pact-foundation/pact';

const { like, eachLike, integer, decimal } = MatchersV3;

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
          discountRate: decimal(0.2),
          usageLimit: integer(100),
          usedCount: integer(0),
        }),
      },
    },
  };
}

interface PublishCouponParams {
  code: string;
  discountRate: number;
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
