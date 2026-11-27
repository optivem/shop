import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import { DemoWakeUp } from '../../features/catalog/DemoWakeUp';

describe('demo wake-up card', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('shows the plain spinner while the catalog loads quickly', () => {
    render(<DemoWakeUp />);
    expect(screen.getByText('Loading products...')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Demo Site Wake Up' })).toBeNull();
  });

  it('explains the demo site and how it is built once the load is slow', () => {
    render(<DemoWakeUp />);
    act(() => {
      vi.advanceTimersByTime(3000);
    });

    expect(screen.getByRole('region', { name: 'Demo Site Wake Up' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Waking up the demo shop…' })).toBeInTheDocument();
    expect(screen.getByLabelText('Slow Loading Hint')).toHaveTextContent('This is a demo site');
    expect(screen.getByRole('status')).toHaveTextContent('Waiting for 3 s');
    expect(screen.getByRole('heading', { name: /Spec-Driven Development with AI/ })).toBeInTheDocument();
    expect(screen.getByText('Production')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Technical highlights' })).toBeInTheDocument();
    expect(screen.getByText('Authentication & security')).toBeInTheDocument();
  });
});
