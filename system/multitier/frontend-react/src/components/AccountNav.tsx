import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router-dom';
import { useRoles } from '../auth/useRoles';
import { useSession } from '../auth/useSession';

/**
 * The side navigation of the signed-in area: "My account" for customers, "Admin" for admins (storefront-ux §8).
 * Admin › All orders is the same /order-history page customers see as My orders.
 */
export function AccountNav() {
  const { t } = useTranslation();
  const session = useSession();
  const { isAdmin, canPlaceOrder } = useRoles();
  if (session.status !== 'signed-in') return null;

  const linkClass = ({ isActive }: { isActive: boolean }) => `list-group-item list-group-item-action${isActive ? ' active' : ''}`;

  return (
    <nav className="account-nav mb-4" aria-label={isAdmin ? t('accountNav.admin') : t('accountNav.myAccount')}>
      <h2 className="account-nav-heading">{isAdmin ? t('accountNav.admin') : t('accountNav.myAccount')}</h2>
      <div className="list-group">
        {isAdmin ? (
          <>
            <NavLink className={linkClass} to="/order-history">
              {t('accountNav.allOrders')}
            </NavLink>
            <NavLink className={linkClass} to="/admin-coupons">
              {t('accountNav.coupons')}
            </NavLink>
          </>
        ) : (
          <>
            <NavLink className={linkClass} to="/order-history">
              {t('accountNav.myOrders')}
            </NavLink>
            <NavLink className={linkClass} to="/account">
              {t('accountNav.profileAddress')}
            </NavLink>
            {canPlaceOrder && (
              <NavLink className={linkClass} to="/new-order">
                {t('accountNav.quickOrder')}
              </NavLink>
            )}
          </>
        )}
      </div>
    </nav>
  );
}
