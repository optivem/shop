import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { OrderHistoryTable } from '../../features/orders';
import type { BrowseOrderHistoryItemResponse } from '../../types/api.types';
import { createMockAuth, renderWithProviders } from '../test-utils';

const order = (overrides: Partial<BrowseOrderHistoryItemResponse> = {}): BrowseOrderHistoryItemResponse => ({
  orderNumber: 'ORD-1',
  orderTimestamp: '2026-01-01T10:00:00Z',
  country: 'US',
  sku: 'SKU-1',
  quantity: 1,
  totalPrice: 10,
  appliedCouponCode: null,
  status: 'PLACED',
  ...overrides,
});

const renderTable = (orders: BrowseOrderHistoryItemResponse[], roles: string[]) =>
  renderWithProviders(
    <OrderHistoryTable orders={orders} filter="" onFilterChange={vi.fn()} isLoading={false} error={null} onRefresh={vi.fn()} />,
    { auth: createMockAuth({}, roles) },
  );

describe('order history customer column', () => {
  it('shows the customer of each order to an admin', () => {
    renderTable([order({ customer: 'customer1' }), order({ orderNumber: 'ORD-2', customer: 'customer2' })], ['ADMIN']);
    expect(screen.getByText('Customer')).toBeInTheDocument();
    expect(screen.getByText('customer1')).toBeInTheDocument();
    expect(screen.getByText('customer2')).toBeInTheDocument();
  });

  it('hides the column from a customer', () => {
    renderTable([order({ customer: 'customer1' })], ['CUSTOMER']);
    expect(screen.queryByText('Customer')).not.toBeInTheDocument();
  });

  it('hides the column when the backend does not report customers', () => {
    renderTable([order()], ['ADMIN']);
    expect(screen.queryByText('Customer')).not.toBeInTheDocument();
    expect(screen.getByText('ORD-1')).toBeInTheDocument();
  });
});
