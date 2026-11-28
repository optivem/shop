import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { RotateCcw, Search, ShoppingCart, Truck } from 'lucide-react';
import { useRoles } from '../auth/useRoles';
import { useSession } from '../auth/useSession';
import { useCart } from '../contexts/useCart';
import { useCatalog } from '../contexts/useCatalog';
import { categoriesOf } from '../features/catalog/catalog-query';
import { CategoryMenu } from './CategoryMenu';
import { LanguageSwitcher } from './LanguageSwitcher';
import { UserMenu } from './UserMenu';

/**
 * The header on every page (storefront-ux §3, Retail look): utility strip, logo, the Categories menu, search
 * (with a category scope), cart, sign-in state, and the category / account nav row. The layout is responsive through CSS only: no element
 * is rendered twice, so the UI drivers' selectors keep matching exactly one element.
 */
export function StoreHeader() {
  const { t } = useTranslation();
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
        <div className="container store-utility-row">
          <span className="store-utility-item">
            <Truck size={14} aria-hidden="true" />
            {t('header.freeShipping')}
          </span>
          <span className="store-utility-item">
            <RotateCcw size={14} aria-hidden="true" />
            {t('header.returns')}
          </span>
          <span className="store-utility-note">{t('header.demoStore')}</span>
          <LanguageSwitcher />
        </div>
      </div>
      <div className="store-header-top">
        <div className="container d-flex flex-wrap align-items-center py-3 store-header-row">
          <CategoryMenu products={products} />
          <Link className="store-logo" to="/">
            My<span className="store-logo-mark">Shop</span>
          </Link>
          <form className="store-search" method="get" action="/" role="search" onSubmit={onSearch}>
            {categories.length > 0 && (
              <select
                name="category"
                className="store-search-scope"
                aria-label={t('header.searchCategory')}
                value={searchCategory}
                onChange={(e) => setSearchCategory(e.target.value)}
              >
                <option value="">{t('header.allCategories')}</option>
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
              placeholder={t('header.searchPlaceholder')}
              aria-label={t('header.searchProducts')}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <button type="submit" className="store-search-button" aria-label={t('header.search')}>
              <Search size={20} strokeWidth={2.2} aria-hidden="true" />
            </button>
          </form>
          <div className="store-header-actions d-flex align-items-center gap-3">
            {session.status === 'anonymous' && (
              <Link
                className="store-header-link store-sign-in"
                to={`/sign-in?returnUrl=${encodeURIComponent(returnUrl)}`}
                data-testid="sign-in"
              >
                <span className="store-header-hint">{t('header.hello')}</span>
                <span>{t('header.signIn')}</span>
              </Link>
            )}
            {session.status === 'anonymous' && (
              // UC-013: Keycloak's registration form, then back here signed in as a new customer.
              <Link
                className="store-header-link store-sign-up"
                to={`/sign-up?returnUrl=${encodeURIComponent(returnUrl)}`}
                data-testid="sign-up"
              >
                <span className="store-header-hint">{t('header.newCustomer')}</span>
                <span>{t('header.signUp')}</span>
              </Link>
            )}
            <UserMenu />
            <Link className="store-header-link store-cart" to="/cart">
              <span className="store-cart-icon">
                <ShoppingCart size={26} strokeWidth={1.8} aria-hidden="true" />
                <span aria-live="polite">
                  {totalQuantity > 0 && (
                    <span className="badge rounded-pill store-cart-badge" aria-label={t('header.cartItemCount')}>
                      {totalQuantity}
                    </span>
                  )}
                </span>
              </span>
              <span className="store-cart-label">{t('header.cart')}</span>
            </Link>
          </div>
        </div>
      </div>
      <nav className="store-header-nav" aria-label={t('header.storeNavigation')}>
        <div className="container store-nav-row">
          <div className="store-nav-group">
            <Link className="store-nav-link" to="/">
              {t('header.allProducts')}
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
                  {t('header.quickOrder')}
                </Link>
              )}
              <Link className="store-nav-link" to="/order-history">
                {isAdmin ? t('header.allOrders') : t('header.myOrders')}
              </Link>
              {isAdmin && (
                <Link className="store-nav-link" to="/admin-coupons">
                  {t('header.admin')}
                </Link>
              )}
            </div>
          )}
        </div>
      </nav>
    </header>
  );
}
