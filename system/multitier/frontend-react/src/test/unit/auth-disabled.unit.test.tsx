import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthGate } from '../../auth/AuthGate';
import { RequireAdmin } from '../../auth/RequireAdmin';
import { getAccessToken } from '../../auth/access-token';
import { NotificationProvider } from '../../contexts/NotificationContext';
import { Navbar } from '../../components/Navbar';
import { Home } from '../../pages/Home';
import { browseCoupons } from '../../services/coupon-service';

vi.mock('../../auth/user-manager', () => ({ userManager: undefined }));

describe('auth disabled (no Keycloak configured)', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('renders the app without redirecting to login', () => {
    render(
      <AuthGate>
        <div>app content</div>
      </AuthGate>,
    );
    expect(screen.getByText('app content')).toBeInTheDocument();
    expect(screen.queryByText(/Redirecting to sign in/)).not.toBeInTheDocument();
  });

  it('sends no Authorization header', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(new Response(JSON.stringify({ coupons: [] }), { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);

    await browseCoupons();

    expect(await getAccessToken()).toBeUndefined();
    expect(new Headers(fetchMock.mock.calls[0]?.[1]?.headers).has('Authorization')).toBe(false);
  });

  it('hides the user menu and logout', () => {
    render(
      <AuthGate>
        <MemoryRouter>
          <Navbar />
        </MemoryRouter>
      </AuthGate>,
    );
    expect(screen.queryByTestId('username')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Logout' })).not.toBeInTheDocument();
  });

  it('shows admin-only UI', () => {
    render(
      <AuthGate>
        <NotificationProvider>
          <MemoryRouter>
            <Home />
          </MemoryRouter>
        </NotificationProvider>
      </AuthGate>,
    );
    expect(screen.getByText('Manage Coupons')).toBeInTheDocument();
  });

  it('lets RequireAdmin through', () => {
    render(
      <AuthGate>
        <MemoryRouter initialEntries={['/admin-coupons']}>
          <Routes>
            <Route path="/admin-coupons" element={<RequireAdmin><div>coupon admin</div></RequireAdmin>} />
          </Routes>
        </MemoryRouter>
      </AuthGate>,
    );
    expect(screen.getByText('coupon admin')).toBeInTheDocument();
  });
});
