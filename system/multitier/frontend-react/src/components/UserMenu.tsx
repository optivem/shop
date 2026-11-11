import { Link } from 'react-router-dom';
import { LogOut } from 'lucide-react';
import { useSession } from '../auth/useSession';

/** Shows the signed-in username (linking to My account) and a logout button. */
export function UserMenu() {
  const session = useSession();
  if (session.status !== 'signed-in' || !session.username) return null;

  return (
    <div className="d-flex align-items-center gap-2">
      <div className="store-header-link store-user">
        <span className="store-header-hint" aria-hidden="true">
          Hello
        </span>
        <Link to="/account" data-testid="username" title="My account">
          {session.username}
        </Link>
      </div>
      <button type="button" className="btn btn-outline-secondary store-logout" onClick={session.signOut}>
        <LogOut size={16} aria-hidden="true" />
        Logout
      </button>
    </div>
  );
}
