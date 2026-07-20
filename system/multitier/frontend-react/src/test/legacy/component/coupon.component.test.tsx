import { describe, it, expect, vi, afterEach } from 'vitest';
import path from 'node:path';
import { PactV3, MatchersV3 } from '@pact-foundation/pact';
import { screen } from '@testing-library/react';
import { AdminCoupons } from '../../../pages/AdminCoupons';
import { renderWithProviders, routeApiTo } from '../../test-utils';

const { like, eachLike, integer, decimal } = MatchersV3;

const provider = new PactV3({
  consumer: 'frontend',
  provider: 'backend',
  dir: path.resolve(process.cwd(), '../../../contracts'),
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('AdminCoupons', () => {
  it('shows coupons when they are returned', async () => {
    provider.addInteraction({
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
    });

    await provider.executeTest(async (mockserver) => {
      routeApiTo(mockserver.url);
      renderWithProviders(<AdminCoupons />);

      expect(await screen.findByText('SAVE10')).toBeInTheDocument();
    });
  });
});
