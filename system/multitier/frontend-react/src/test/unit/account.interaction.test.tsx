// My account › Profile & address (UC-010), Quick order prefill, and the admin customer filter (UC-011).
import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Account } from '../../pages/Account';
import { OrderHistory } from '../../pages/OrderHistory';
import { QuickOrder } from '../../pages/QuickOrder';
import { orderService } from '../../services/order-service';
import { profileService } from '../../services/profile-service';
import type { BrowseOrderHistoryItemResponse, ProfileResponse } from '../../types/api.types';
import { createMockAuth, renderWithProviders } from '../test-utils';

const profile: ProfileResponse = {
  firstName: 'Ana',
  lastName: 'Novak',
  email: 'ana.novak@example.com',
  street: 'Main St 1',
  postalCode: '10115',
  city: 'Berlin',
  country: 'DE',
};

const order = (orderNumber: string, customer: string | null): BrowseOrderHistoryItemResponse => ({
  orderNumber,
  orderTimestamp: '2026-11-01T09:00:00Z',
  country: 'US',
  sku: 'HP-15',
  quantity: 1,
  totalPrice: '699.99',
  appliedCouponCode: null,
  status: 'PLACED',
  customer,
});

afterEach(() => vi.restoreAllMocks());

describe('profile page', () => {
  it('shows the saved profile with a read-only email, the country choices and the tax hint', async () => {
    vi.spyOn(profileService, 'viewProfile').mockResolvedValue({ success: true, data: profile });
    renderWithProviders(<Account />, { auth: createMockAuth({}, ['CUSTOMER']) });
    expect(await screen.findByLabelText('First Name')).toHaveValue('Ana');
    expect(screen.getByLabelText('Email')).toHaveValue('ana.novak@example.com');
    expect(screen.getByLabelText('Email')).toHaveAttribute('readonly');
    expect(screen.getByLabelText('Street')).toHaveValue('Main St 1');
    expect(screen.getByLabelText('Profile Country')).toHaveValue('DE');
    const choices = Array.from(screen.getByLabelText<HTMLSelectElement>('Profile Country').options).map((o) => o.value);
    expect(choices).toEqual(['', 'US', 'DE', 'GB']);
    expect(screen.getByLabelText('Tax Country Hint')).toHaveTextContent("Sales tax is charged at the rate of your shipping address's country.");
  });

  it('saves the profile and shows the saved values', async () => {
    vi.spyOn(profileService, 'viewProfile').mockResolvedValue({ success: true, data: profile });
    const saved = { ...profile, street: '1 High St', postalCode: 'SW1A 1AA', city: 'London', country: 'GB' };
    const update = vi.spyOn(profileService, 'updateProfile').mockResolvedValue({ success: true, data: saved });
    renderWithProviders(<Account />, { auth: createMockAuth({}, ['CUSTOMER']) });
    const street = await screen.findByLabelText('Street');
    await userEvent.clear(street);
    await userEvent.type(street, '1 High St');
    await userEvent.selectOptions(screen.getByLabelText('Profile Country'), 'GB');
    await userEvent.click(screen.getByRole('button', { name: 'Save Profile' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Your profile has been saved.');
    expect(update).toHaveBeenCalledWith(expect.objectContaining({ street: '1 High St', country: 'GB' }));
    expect(screen.getByLabelText('City')).toHaveValue('London');
  });

  it("shows the backend's field errors when the profile is rejected", async () => {
    vi.spyOn(profileService, 'viewProfile').mockResolvedValue({ success: true, data: profile });
    vi.spyOn(profileService, 'updateProfile').mockResolvedValue({
      success: false,
      error: { message: 'The request contains one or more validation errors', fieldErrors: ['city: City is required'] },
    });
    renderWithProviders(<Account />, { auth: createMockAuth({}, ['CUSTOMER']) });
    await userEvent.clear(await screen.findByLabelText('City'));
    await userEvent.click(screen.getByRole('button', { name: 'Save Profile' }));
    expect(await screen.findByText('city: City is required')).toHaveClass('field-error');
  });
});

describe('quick order', () => {
  it('prefills the country from the profile', async () => {
    vi.spyOn(profileService, 'viewProfile').mockResolvedValue({ success: true, data: { ...profile, country: 'GB' } });
    renderWithProviders(<QuickOrder />, { auth: createMockAuth({}, ['CUSTOMER']) });
    expect(await screen.findByLabelText('Country')).toHaveValue('GB');
  });

  it('starts from US without a saved country', async () => {
    vi.spyOn(profileService, 'viewProfile').mockResolvedValue({ success: false, error: { message: 'down', status: 503 } });
    renderWithProviders(<QuickOrder />, { auth: createMockAuth({}, ['CUSTOMER']) });
    expect(await screen.findByLabelText('Country')).toHaveValue('US');
  });
});

describe('all orders (admin)', () => {
  it('offers the customers who have ordered, and filters by one', async () => {
    const browse = vi.spyOn(orderService, 'browseOrderHistory').mockImplementation((query) => {
      const { customer, page } = typeof query === 'object' ? query : {};
      if (page === 2) return Promise.resolve({ success: true, data: { orders: [order('ORD-1001', 'alice')], totalPages: 2 } });
      if (customer === 'alice') return Promise.resolve({ success: true, data: { orders: [order('ORD-1003', 'alice')] } });
      return Promise.resolve({
        success: true,
        data: { orders: [order('ORD-1013', 'carol'), order('ORD-1002', 'bob'), order('ORD-0999', null)], totalPages: 2 },
      });
    });
    renderWithProviders(<OrderHistory />, { auth: createMockAuth({}, ['ADMIN']) });
    const filter = await screen.findByLabelText('Customer Filter');
    await waitFor(() => {
      const options = Array.from((filter as HTMLSelectElement).options).map((o) => o.textContent);
      expect(options).toEqual(['All customers', 'alice', 'bob', 'carol']);
    });
    expect(filter).toHaveValue('');

    await userEvent.selectOptions(filter, 'alice');
    expect(await screen.findByText('ORD-1003')).toBeInTheDocument();
    expect(browse).toHaveBeenCalledWith({ orderNumber: '', customer: 'alice' });
  });

  it('offers a customer neither the filter nor the customer column', async () => {
    vi.spyOn(orderService, 'browseOrderHistory').mockResolvedValue({ success: true, data: { orders: [order('ORD-1', 'alice')] } });
    renderWithProviders(<OrderHistory />, { auth: createMockAuth({}, ['CUSTOMER']) });
    expect(await screen.findByText('ORD-1')).toBeInTheDocument();
    expect(screen.queryByLabelText('Customer Filter')).not.toBeInTheDocument();
    expect(screen.queryByText('Customer')).not.toBeInTheDocument();
  });
});
