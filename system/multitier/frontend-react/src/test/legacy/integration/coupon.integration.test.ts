import { describe, it, expect, afterEach, vi } from 'vitest';
import path from 'node:path';
import { PactV3 } from '@pact-foundation/pact';
import { browseCoupons, createCoupon } from '../../../services/coupon-service';
import { browseCouponsInteraction, publishCouponInteraction } from '../../interactions/coupon.interactions';
import { routeApiTo } from '../../test-utils';

const provider = new PactV3({
  consumer: 'frontend',
  provider: 'backend',
  dir: path.resolve(process.cwd(), '../../../contracts'),
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('coupon gateway narrow integration', () => {
  it('browses coupons via browseCoupons directly', async () => {
    provider.addInteraction(browseCouponsInteraction());

    await provider.executeTest(async (mockserver) => {
      routeApiTo(mockserver.url);
      const result = await browseCoupons();

      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.coupons.length).toBeGreaterThan(0);
      }
    });
  });

  it('publishes a coupon via createCoupon directly', async () => {
    provider.addInteraction(publishCouponInteraction({ code: 'SAVE10', discountRate: '0.2' }));

    await provider.executeTest(async (mockserver) => {
      routeApiTo(mockserver.url);
      const result = await createCoupon('SAVE10', '0.2', null, null, null);

      expect(result.success).toBe(true);
    });
  });
});
