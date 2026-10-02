import { useAuth } from 'react-oidc-context';

/** Shows the signed-in username and a logout button. */
export function UserMenu() {
  const auth = useAuth();
  if (!auth.isAuthenticated) return null;

  const profile = auth.user?.profile;
  const username = profile?.preferred_username ?? profile?.name ?? profile?.sub;

  return (
    <div className="d-flex align-items-center ms-auto">
      <span className="navbar-text text-white me-3" data-testid="username">
        {username}
      </span>
      <button type="button" className="btn btn-outline-light btn-sm" onClick={() => void auth.signoutRedirect()}>
        Logout
      </button>
    </div>
  );
}
