import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AuthContext, type AuthContextProps } from 'react-oidc-context';
import { RequireAuth } from '../../auth/AuthGate';
import { setAccessTokenSource, setUnauthorizedHandler } from '../../auth/access-token';
import { getAuthConfig, getAuthority } from '../../auth/config';
import { Navbar } from '../../components/Navbar';
import { browseCoupons } from '../../services/coupon-service';
import { createMockAuth } from '../test-utils';

function renderWithAuth(ui: React.ReactElement, auth: AuthContextProps) {
  return render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter>{ui}</MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('RequireAuth', () => {
  it('redirects an unauthenticated visitor to login and hides the content', () => {
    const signinRedirect = vi.fn().mockResolvedValue(undefined);
    const auth = createMockAuth({ isAuthenticated: false, user: null, signinRedirect });

    renderWithAuth(<RequireAuth><div>secret</div></RequireAuth>, auth);

    expect(signinRedirect).toHaveBeenCalledTimes(1);
    expect(screen.queryByText('secret')).not.toBeInTheDocument();
  });

  it('renders the content when authenticated without redirecting', () => {
    const signinRedirect = vi.fn().mockResolvedValue(undefined);
    const auth = createMockAuth({ signinRedirect });

    renderWithAuth(<RequireAuth><div>secret</div></RequireAuth>, auth);

    expect(screen.getByText('secret')).toBeInTheDocument();
    expect(signinRedirect).not.toHaveBeenCalled();
  });

  it('does not redirect while the login callback is being processed', () => {
    const signinRedirect = vi.fn().mockResolvedValue(undefined);
    const auth = createMockAuth({ isAuthenticated: false, isLoading: true, user: null, signinRedirect });

    renderWithAuth(<RequireAuth><div>secret</div></RequireAuth>, auth);

    expect(signinRedirect).not.toHaveBeenCalled();
  });
});

describe('API client authentication', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    setAccessTokenSource(() => Promise.resolve(undefined));
    setUnauthorizedHandler(() => undefined);
  });

  it('attaches the bearer token to API calls', async () => {
    setAccessTokenSource(() => Promise.resolve('abc123'));
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(new Response(JSON.stringify({ coupons: [] }), { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);

    await browseCoupons();

    const init = fetchMock.mock.calls[0]?.[1];
    expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer abc123');
  });

  it('sends no Authorization header when there is no token', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(new Response(JSON.stringify({ coupons: [] }), { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);

    await browseCoupons();

    const init = fetchMock.mock.calls[0]?.[1];
    expect(new Headers(init?.headers).has('Authorization')).toBe(false);
  });

  it('triggers re-login on a 401 response', async () => {
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{}', { status: 401 })));

    const result = await browseCoupons();

    expect(onUnauthorized).toHaveBeenCalledTimes(1);
    expect(result.success).toBe(false);
  });
});

describe('Navbar user menu', () => {
  it('shows the username and signs out on logout', async () => {
    const signoutRedirect = vi.fn().mockResolvedValue(undefined);
    const auth = createMockAuth({ signoutRedirect });

    renderWithAuth(<Navbar />, auth);
    expect(screen.getByTestId('username')).toHaveTextContent('customer1');
    await userEvent.click(screen.getByRole('button', { name: 'Logout' }));

    expect(signoutRedirect).toHaveBeenCalledTimes(1);
  });
});

describe('auth config', () => {
  it('uses local defaults when no runtime config is present', () => {
    expect(getAuthority(getAuthConfig({}, true)!)).toBe('http://localhost:8191/realms/shop');
  });

  it('builds the authority from runtime values', () => {
    const config = getAuthConfig({ keycloakUrl: 'http://kc:8080/', keycloakRealm: 'r', keycloakClientId: 'c' }, false)!;
    expect(getAuthority(config)).toBe('http://kc:8080/realms/r');
    expect(config.clientId).toBe('c');
  });
});

describe('auth disabled', () => {
  it('is disabled when the runtime config has no Keycloak URL outside dev', () => {
    expect(getAuthConfig({}, false)).toBeUndefined();
    expect(getAuthConfig({ keycloakUrl: '', keycloakRealm: '', keycloakClientId: '' }, false)).toBeUndefined();
    expect(getAuthConfig(undefined, false)).toBeUndefined();
  });

  it('is enabled when a Keycloak URL is configured', () => {
    expect(getAuthConfig({ keycloakUrl: 'http://kc:8080' }, false)?.keycloakUrl).toBe('http://kc:8080');
  });
});
