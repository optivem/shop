// Shared helpers for component and Pact consumer tests.
import type { ReactElement } from 'react';
import { render, type RenderOptions } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { vi } from 'vitest';
import { NotificationProvider } from '../contexts/NotificationContext';

/**
 * Render a page/component inside the app-level providers it expects:
 * a router (Navbar/links/useParams) and the NotificationProvider.
 *
 * Pass `routePath`/`initialEntry` to drive route params (e.g. OrderDetails
 * which reads `:orderNumber` via useParams).
 */
export function renderWithProviders(
  ui: ReactElement,
  opts: { routePath?: string; initialEntry?: string } & Omit<RenderOptions, 'wrapper'> = {},
) {
  const { routePath, initialEntry = '/', ...rtlOpts } = opts;
  return render(
    <NotificationProvider>
      <MemoryRouter initialEntries={[initialEntry]}>
        {routePath ? (
          <Routes>
            <Route path={routePath} element={ui} />
          </Routes>
        ) : (
          ui
        )}
      </MemoryRouter>
    </NotificationProvider>,
    rtlOpts,
  );
}

/**
 * Route the production services' relative `/api/*` calls to an absolute base
 * URL (e.g. a Pact mock server). Production code calls `fetch('/api/orders')`
 * with a relative URL; under jsdom that has no origin, so we rewrite it.
 *
 * Returns a restore function; pair it with vi.unstubAllGlobals() in afterEach.
 */
export function routeApiTo(baseUrl: string): void {
  const realFetch = globalThis.fetch.bind(globalThis);
  vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => {
    const urlFromInput = input instanceof URL ? input.href : (input as Request).url;
    let url = typeof input === 'string' ? input : urlFromInput;
    if (url.startsWith('/')) {
      url = baseUrl.replace(/\/$/, '') + url;
    }
    return realFetch(url, init);
  });
}
