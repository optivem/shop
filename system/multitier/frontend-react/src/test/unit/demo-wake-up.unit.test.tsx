import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import { DemoWakeUp } from '../../features/catalog/DemoWakeUp';

/** Answers /api/status/<system> with UP or DOWN, as the backend does once it is running. */
function backendAnswering(up: Record<string, boolean>) {
  return vi.fn(async (url: string) => {
    const system = url.split('/').pop() ?? '';
    const isUp = up[system] ?? false;
    return new Response(JSON.stringify({ status: isUp ? 'UP' : 'DOWN' }), {
      status: isUp ? 200 : 503,
      headers: { 'Content-Type': 'application/json' },
    });
  });
}

function nodeStatus(name: string) {
  return screen.getByText(name).closest('.wake-up-node');
}

async function flush(ms = 0) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms);
  });
}

describe('demo wake-up card', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('shows the wake-up card from the first moment of the load', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => undefined)));
    render(<DemoWakeUp />);
    expect(screen.getByRole('dialog', { name: 'Demo Site Wake Up' })).toBeInTheDocument();
    expect(screen.queryByText('Loading products...')).toBeNull();
  });

  it('explains the demo site while the services wake up', async () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => undefined)));
    render(<DemoWakeUp />);
    await flush(3000);

    expect(screen.getByRole('heading', { name: 'Waking up the demo shop…' })).toBeInTheDocument();
    expect(screen.getByLabelText('Slow Loading Hint')).toHaveTextContent('This is a demo site');
    expect(screen.getByRole('status')).toHaveTextContent('Waiting for 3 s');
    expect(screen.getByText('Demo shop by Valentina Jemuović, Optivem')).toBeInTheDocument();
    expect(nodeStatus('Shop backend')).toHaveTextContent('Waking up…');
  });

  it('turns each part green as the backend reports it up, and keeps asking about the rest', async () => {
    const fetchMock = backendAnswering({ backend: true, erp: true, clock: true, tax: false });
    vi.stubGlobal('fetch', fetchMock);
    render(<DemoWakeUp />);
    await flush();

    expect(nodeStatus('Shop backend')).toHaveTextContent('Ready');
    expect(nodeStatus('Database')).toHaveTextContent('Ready');
    expect(nodeStatus('ERP')).toHaveTextContent('Ready');
    expect(nodeStatus('Clock')).toHaveTextContent('Ready');
    expect(nodeStatus('Tax')).toHaveTextContent('Waking up…');

    const taxCalls = () => fetchMock.mock.calls.filter(([url]) => url === '/api/status/tax').length;
    const before = taxCalls();
    await flush(2000);
    expect(taxCalls()).toBe(before + 1);
  });

  it('says a part is not responding after a minute without an answer', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Promise.reject(new TypeError('Failed to fetch'))));
    render(<DemoWakeUp />);
    await flush(61_000);

    expect(nodeStatus('Shop backend')).toHaveTextContent('Not responding');
    expect(nodeStatus('ERP')).toHaveTextContent('Not responding');
  });
});
