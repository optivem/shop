import { NavLink } from 'react-router-dom';
import { useRoles } from '../auth/useRoles';
import { useSession } from '../auth/useSession';

/**
 * The side navigation of the signed-in area: "My account" for customers, "Admin" for admins (storefront-ux §8).
 * Admin › All orders is the same /order-history page customers see as My orders.
 */
export function AccountNav() {
  const session = useSession();
  const { isAdmin, canPlaceOrder } = useRoles();
  if (session.status !== 'signed-in') return null;

  const linkClass = ({ isActive }: { isActive: boolean }) => `list-group-item list-group-item-action${isActive ? ' active' : ''}`;

  return (
    <nav className="account-nav mb-4" aria-label={isAdmin ? 'Admin' : 'My account'}>
      <h2 className="h6 text-uppercase text-muted">{isAdmin ? 'Admin' : 'My account'}</h2>
      <div className="list-group">
        {isAdmin ? (
          <>
            <NavLink className={linkClass} to="/order-history">
              All orders
            </NavLink>
            <NavLink className={linkClass} to="/admin-coupons">
              Coupons
            </NavLink>
          </>
        ) : (
          <>
            <NavLink className={linkClass} to="/order-history">
              My orders
            </NavLink>
            <NavLink className={linkClass} to="/account">
              Profile &amp; address
            </NavLink>
            {canPlaceOrder && (
              <NavLink className={linkClass} to="/new-order">
                Quick order
              </NavLink>
            )}
          </>
        )}
      </div>
    </nav>
  );
}
