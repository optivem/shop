// Shared helpers for component and Pact consumer tests.
import type { ReactElement } from 'react';
import { render, type RenderOptions } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { vi } from 'vitest';
import { AuthContext, type AuthContextProps } from 'react-oidc-context';
import type { User } from 'oidc-client-ts';
import { NotificationProvider } from '../contexts/NotificationContext';

function fakeJwt(roles: string[]): string {
  const encode = (o: object) => btoa(JSON.stringify(o)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
  return `${encode({ alg: 'none' })}.${encode({ realm_access: { roles } })}.sig`;
}

/**
 * An authenticated auth context for tests; override fields (e.g. signoutRedirect) as needed.
 * `roles` goes into the access token's realm_access.roles; defaults to an admin.
 */
export function createMockAuth(
  overrides: Partial<AuthContextProps> = {},
  roles: string[] = ['ADMIN', 'CUSTOMER'],
): AuthContextProps {
  const user = { access_token: fakeJwt(roles), profile: { sub: 'u1', preferred_username: 'customer1' } } as User;
  return {
    isLoading: false,
    isAuthenticated: true,
    user,
    signinRedirect: vi.fn().mockResolvedValue(undefined),
    signoutRedirect: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  } as unknown as AuthContextProps;
}

/**
 * Render a page/component inside the app-level providers it expects:
 * a router (Navbar/links/useParams) and the NotificationProvider.
 *
 * Pass `routePath`/`initialEntry` to drive route params (e.g. OrderDetails
 * which reads `:orderNumber` via useParams).
 */
export function renderWithProviders(
  ui: ReactElement,
  opts: { routePath?: string; initialEntry?: string; auth?: AuthContextProps } & Omit<RenderOptions, 'wrapper'> = {},
) {
  const { routePath, initialEntry = '/', auth = createMockAuth(), ...rtlOpts } = opts;
  return render(
    <AuthContext.Provider value={auth}>
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
      </NotificationProvider>
    </AuthContext.Provider>,
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
