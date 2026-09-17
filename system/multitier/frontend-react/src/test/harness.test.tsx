// Smoke test: proves the Vitest + RTL + jsdom harness is wired up correctly,
// with no backend and no network. If this is green, the harness works.
import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { OrderForm } from '../features/orders/OrderForm';
import { renderWithProviders } from './test-utils';

describe('test harness', () => {
  it('renders a component with providers (no backend)', () => {
    renderWithProviders(
      <OrderForm
        formData={{ sku: '', quantity: 0, quantityValue: '', country: 'US', couponCode: '' }}
        onFormChange={vi.fn()}
        onSubmit={vi.fn()}
        isSubmitting={false}
      />,
    );

    expect(screen.getByRole('heading', { name: 'Place Your Order' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Place Order' })).toBeInTheDocument();
  });
});
