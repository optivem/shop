import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { RequireAdmin } from '../../auth/RequireAdmin';
import { RequireCanPlaceOrder } from '../../auth/RequireCanPlaceOrder';
import { getRolesFromToken } from '../../auth/roles';
import { StoreHeader } from '../../components/StoreHeader';
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
    renderWithProviders(<StoreHeader />, { auth: customer() });
    expect(screen.getByRole('link', { name: 'My orders' })).toHaveAttribute('href', '/order-history');
    expect(screen.queryByRole('link', { name: 'Admin' })).not.toBeInTheDocument();
  });

  it('shows coupon management to an admin', () => {
    renderWithProviders(<StoreHeader />);
    expect(screen.getByRole('link', { name: 'Admin' })).toHaveAttribute('href', '/admin-coupons');
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


describe('place-order access', () => {
  const adminOnly = () => createMockAuth({}, ['ADMIN']);
  const newOrderRoute = (
    <Routes>
      <Route path="/new-order" element={<RequireCanPlaceOrder><div>order form</div></RequireCanPlaceOrder>} />
    </Routes>
  );

  it('hides new order from an admin who is not a customer', () => {
    renderWithProviders(<StoreHeader />, { auth: adminOnly() });
    expect(screen.queryByRole('link', { name: 'Quick order' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Admin' })).toBeInTheDocument();
  });

  it('shows new order to a customer', () => {
    renderWithProviders(<StoreHeader />, { auth: customer() });
    expect(screen.getByRole('link', { name: 'Quick order' })).toHaveAttribute('href', '/new-order');
  });

  it('blocks an admin-only user from the new order route', () => {
    renderWithProviders(newOrderRoute, { initialEntry: '/new-order', auth: adminOnly() });
    expect(screen.getByText('Not authorized')).toBeInTheDocument();
    expect(screen.queryByText('order form')).not.toBeInTheDocument();
  });

  it('lets a customer reach the new order route', () => {
    renderWithProviders(newOrderRoute, { initialEntry: '/new-order', auth: customer() });
    expect(screen.getByText('order form')).toBeInTheDocument();
  });
});
