import { Link } from 'react-router-dom';
import { useSession } from '../auth/useSession';

/** Shows the signed-in username (linking to My account) and a logout button. */
export function UserMenu() {
  const session = useSession();
  if (session.status !== 'signed-in' || !session.username) return null;

  return (
    <div className="d-flex align-items-center">
      <Link className="store-header-link me-3" to="/account" data-testid="username" title="My account">
        {session.username}
      </Link>
      <button type="button" className="btn btn-outline-light btn-sm" onClick={session.signOut}>
        Logout
      </button>
    </div>
  );
}
