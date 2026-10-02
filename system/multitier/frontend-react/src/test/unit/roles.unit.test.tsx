import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { RequireAdmin } from '../../auth/RequireAdmin';
import { getRolesFromToken } from '../../auth/roles';
import { Home } from '../../pages/Home';
import { createMockAuth, renderWithProviders } from '../test-utils';

const customer = () => createMockAuth({}, ['CUSTOMER']);

describe('getRolesFromToken', () => {
  it('returns no roles for missing or malformed tokens', () => {
    expect(getRolesFromToken(undefined)).toEqual([]);
    expect(getRolesFromToken('garbage')).toEqual([]);
  });
});

describe('role-aware navigation', () => {
  it('hides coupon management from a customer', () => {
    renderWithProviders(<Home />, { auth: customer() });
    expect(screen.getByText('View Orders')).toBeInTheDocument();
    expect(screen.queryByText('Manage Coupons')).not.toBeInTheDocument();
  });

  it('shows coupon management to an admin', () => {
    renderWithProviders(<Home />);
    expect(screen.getByText('Manage Coupons')).toBeInTheDocument();
  });
});

describe('RequireAdmin', () => {
  const ui = (
    <Routes>
      <Route path="/admin-coupons" element={<RequireAdmin><div>coupon admin</div></RequireAdmin>} />
    </Routes>
  );

  it('blocks a customer with a Not authorized page', () => {
    renderWithProviders(ui, { initialEntry: '/admin-coupons', auth: customer() });
    expect(screen.getByText('Not authorized')).toBeInTheDocument();
    expect(screen.queryByText('coupon admin')).not.toBeInTheDocument();
  });

  it('lets an admin through', () => {
    renderWithProviders(ui, { initialEntry: '/admin-coupons' });
    expect(screen.getByText('coupon admin')).toBeInTheDocument();
  });
});

