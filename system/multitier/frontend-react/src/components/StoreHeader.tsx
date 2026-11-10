import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useRoles } from '../auth/useRoles';
import { useSession } from '../auth/useSession';
import { useCart } from '../contexts/useCart';
import { useCatalog } from '../contexts/useCatalog';
import { categoriesOf } from '../features/catalog/catalog-query';
import { UserMenu } from './UserMenu';

/** The header on every page (storefront-ux §3): logo, search, cart, sign-in state, categories and account links. */
export function StoreHeader() {
  const session = useSession();
  const { isAdmin, canPlaceOrder } = useRoles();
  const { totalQuantity } = useCart();
  const { products } = useCatalog();
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const currentQuery = location.pathname === '/' ? (searchParams.get('q') ?? '') : '';
  const [search, setSearch] = useState(currentQuery);
  const [shownQuery, setShownQuery] = useState(currentQuery);

  // Follow the address when it changes (back button, "Clear filters"), without an effect.
  if (shownQuery !== currentQuery) {
    setShownQuery(currentQuery);
    setSearch(currentQuery);
  }

  // Plain GET /?q= for the address; inside the SPA it just moves the router.
  const onSearch = (event: FormEvent) => {
    event.preventDefault();
    const term = search.trim();
    navigate(term ? `/?q=${encodeURIComponent(term)}` : '/');
  };

  const returnUrl = `${location.pathname}${location.search}`;

  return (
    <header className="store-header">
      <div className="store-header-top">
        <div className="container d-flex flex-wrap align-items-center gap-3 py-2">
          <Link className="store-logo" to="/">
            <span className="store-logo-mark" aria-hidden="true">■</span> MyShop
          </Link>
          <form className="store-search d-flex flex-grow-1" method="get" action="/" role="search" onSubmit={onSearch}>
            <input
              type="search"
              name="q"
              className="form-control"
              placeholder="Search products…"
              aria-label="Search Products"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <button type="submit" className="btn btn-light ms-2" aria-label="Search">
              Search
            </button>
          </form>
          <div className="d-flex align-items-center gap-3 ms-auto">
            <Link className="store-header-link" to="/cart" aria-label="Cart">
              <span aria-hidden="true">🛒</span> Cart
              <span aria-live="polite">
                {totalQuantity > 0 && (
                  <span className="badge rounded-pill bg-warning text-dark ms-1" aria-label="Cart Item Count">
                    {totalQuantity}
                  </span>
                )}
              </span>
            </Link>
            {session.status === 'anonymous' && (
              <Link
                className="store-header-link"
                to={`/sign-in?returnUrl=${encodeURIComponent(returnUrl)}`}
                aria-label="Sign In"
              >
                Sign in
              </Link>
            )}
            <UserMenu />
          </div>
        </div>
      </div>
      <nav className="store-header-nav" aria-label="Store navigation">
        <div className="container d-flex flex-wrap align-items-center gap-3 py-1">
          <Link className="store-nav-link" to="/">
            All products
          </Link>
          {categoriesOf(products).map((category) => (
            <Link key={category} className="store-nav-link" to={`/?category=${encodeURIComponent(category)}`}>
              {category}
            </Link>
          ))}
          {session.status === 'signed-in' && (
            <div className="d-flex flex-wrap gap-3 ms-auto">
              {canPlaceOrder && (
                <Link className="store-nav-link" to="/new-order">
                  Quick order
                </Link>
              )}
              <Link className="store-nav-link" to="/order-history">
                {isAdmin ? 'All orders' : 'My orders'}
              </Link>
              {isAdmin && (
                <Link className="store-nav-link" to="/admin-coupons">
                  Admin
                </Link>
              )}
            </div>
          )}
        </div>
      </nav>
    </header>
  );
}
