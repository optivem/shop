import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { ShoppingCart, Truck } from 'lucide-react';
import { useRoles } from '../auth/useRoles';
import { useSession } from '../auth/useSession';
import { useCart } from '../contexts/useCart';
import { useCatalog } from '../contexts/useCatalog';
import { categoriesOf } from '../features/catalog/catalog-query';
import { UserMenu } from './UserMenu';

/**
 * The header on every page (storefront-ux §3): utility strip, logo, search (with a category scope), cart,
 * sign-in state, and the category / account nav row. The layout is responsive through CSS only: no element
 * is rendered twice, so the UI drivers' selectors keep matching exactly one element.
 */
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
  // '' searches all categories; the scope only changes when the visitor picks one.
  const [searchCategory, setSearchCategory] = useState('');
  const categories = categoriesOf(products);

  // Follow the address when it changes (back button, "Clear filters"), without an effect.
  if (shownQuery !== currentQuery) {
    setShownQuery(currentQuery);
    setSearch(currentQuery);
  }

  // Plain GET /?q=&category= for the address; inside the SPA it just moves the router.
  const onSearch = (event: FormEvent) => {
    event.preventDefault();
    const params = new URLSearchParams();
    const term = search.trim();
    if (term) params.set('q', term);
    if (searchCategory) params.set('category', searchCategory);
    const query = params.toString();
    navigate(query ? `/?${query}` : '/');
  };

  const returnUrl = `${location.pathname}${location.search}`;

  return (
    <header className="store-header">
      <div className="store-utility">
        <div className="container d-flex align-items-center gap-2 py-2">
          <Truck size={14} aria-hidden="true" />
          <span>Free shipping over $1,000</span>
        </div>
      </div>
      <div className="store-header-top">
        <div className="container d-flex flex-wrap align-items-center py-3 store-header-row">
          <Link className="store-logo" to="/">
            MyShop<span className="store-logo-mark" aria-hidden="true">.</span>
          </Link>
          <form className="store-search" method="get" action="/" role="search" onSubmit={onSearch}>
            {categories.length > 0 && (
              <select
                name="category"
                className="store-search-scope"
                aria-label="Search category"
                value={searchCategory}
                onChange={(e) => setSearchCategory(e.target.value)}
              >
                <option value="">All</option>
                {categories.map((category) => (
                  <option key={category} value={category}>
                    {category}
                  </option>
                ))}
              </select>
            )}
            <input
              type="search"
              name="q"
              className="store-search-input"
              placeholder="Search products…"
              aria-label="Search Products"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <button type="submit" className="store-search-button" aria-label="Search">
              Search
            </button>
          </form>
          <div className="store-header-actions d-flex align-items-center gap-3">
            {session.status === 'anonymous' && (
              <Link
                className="store-header-link store-sign-in"
                to={`/sign-in?returnUrl=${encodeURIComponent(returnUrl)}`}
                aria-label="Sign In"
              >
                <span className="store-header-hint">Hello</span>
                <span>Sign in</span>
              </Link>
            )}
            <UserMenu />
            <Link className="store-header-link store-cart" to="/cart" aria-label="Cart">
              <span className="store-cart-icon">
                <ShoppingCart size={26} strokeWidth={1.8} aria-hidden="true" />
                <span aria-live="polite">
                  {totalQuantity > 0 && (
                    <span className="badge rounded-pill store-cart-badge" aria-label="Cart Item Count">
                      {totalQuantity}
                    </span>
                  )}
                </span>
              </span>
              <span className="store-cart-label">Cart</span>
            </Link>
          </div>
        </div>
      </div>
      <nav className="store-header-nav" aria-label="Store navigation">
        <div className="container store-nav-row">
          <div className="store-nav-group">
            <Link className="store-nav-link" to="/">
              All products
            </Link>
            {categories.map((category) => (
              <Link key={category} className="store-nav-link" to={`/?category=${encodeURIComponent(category)}`}>
                {category}
              </Link>
            ))}
          </div>
          {session.status === 'signed-in' && (
            <div className="store-nav-group store-nav-account">
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
