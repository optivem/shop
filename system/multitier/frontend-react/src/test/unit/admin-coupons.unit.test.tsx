// Regression tests for two AdminCoupons UI fixes: a failed coupon load surfaces its
// error to the user, and a failed coupon save leaves the form's input intact instead
// of silently clearing it. Both drive the real AdminCoupons page through
// renderWithProviders (the same rendering harness harness.test.tsx uses), with the
// coupon-service module mocked directly — no backend or mock server needed.
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AdminCoupons } from '../../pages/AdminCoupons';
import { renderWithProviders } from '../test-utils';
import * as couponService from '../../services/coupon-service';

vi.mock('../../services/coupon-service', () => ({
  browseCoupons: vi.fn(),
  createCoupon: vi.fn(),
}));

const browseCoupons = vi.mocked(couponService.browseCoupons);
const createCoupon = vi.mocked(couponService.createCoupon);

describe('AdminCoupons', () => {
  beforeEach(() => {
    browseCoupons.mockReset();
    createCoupon.mockReset();
  });

  it('shows the coupon-load error to the user when loading coupons fails', async () => {
    browseCoupons.mockResolvedValue({
      success: false,
      error: { message: 'Network error: fetch failed', status: 0 },
    });

    renderWithProviders(<AdminCoupons />);

    expect(await screen.findByText('Failed to load coupons')).toBeInTheDocument();
  });

  it('keeps the form input after a failed save', async () => {
    browseCoupons.mockResolvedValue({ success: true, data: { coupons: [] } });
    createCoupon.mockResolvedValue({
      success: false,
      error: { message: 'Coupon code SAVE10 already exists', status: 422 },
    });

    const user = userEvent.setup();
    renderWithProviders(<AdminCoupons />);

    const codeField = await screen.findByLabelText('Coupon Code');
    await user.clear(codeField);
    await user.type(codeField, 'SAVE10');
    await user.click(screen.getByRole('button', { name: 'Create Coupon' }));

    expect(await screen.findByText('Coupon code SAVE10 already exists')).toBeInTheDocument();
    expect(codeField).toHaveValue('SAVE10');
  });
});
