"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { categoriesOf } from "@/lib/storefront/catalog-query";
import { Icon } from "./Icon";
import { useCart, useCatalog, useSession } from "./StorefrontProviders";

/** Shows the signed-in username (linking to My account) and a logout button. */
function UserMenu({ username }: Readonly<{ username: string }>) {
  return (
    <form method="post" action="/logout" className="d-flex align-items-center gap-2">
      <div className="store-header-link store-user">
        <span className="store-header-hint" aria-hidden="true">
          Hello
        </span>
        <Link href="/account" data-testid="username" title="My account">
          {username}
        </Link>
      </div>
      <button type="submit" className="btn btn-outline-secondary store-logout">
        <Icon name="log-out" />
        Logout
      </button>
    </form>
  );
}

/**
 * The header on every page (storefront-ux §3): utility strip, logo, search (with a category scope), cart,
 * sign-in state, and the category / account nav row. The layout is responsive through CSS only: no element
 * is rendered twice, so the UI drivers' selectors keep matching exactly one element.
 */
export function StoreHeader() {
  const session = useSession();
  const { totalQuantity } = useCart();
  const { products } = useCatalog();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const router = useRouter();
  const currentQuery = pathname === "/" ? (searchParams.get("q") ?? "") : "";
  const [search, setSearch] = useState(currentQuery);
  const [shownQuery, setShownQuery] = useState(currentQuery);
  // '' searches all categories; the scope only changes when the visitor picks one.
  const [searchCategory, setSearchCategory] = useState("");
  const categories = categoriesOf(products);

  // Follow the address when it changes (back button, "Clear filters"), without an effect.
  if (shownQuery !== currentQuery) {
    setShownQuery(currentQuery);
    setSearch(currentQuery);
  }

  // A plain GET /?q=&category= without JavaScript; with it, a client-side navigation.
  const onSearch = (event: FormEvent) => {
    event.preventDefault();
    const params = new URLSearchParams();
    const term = search.trim();
    if (term) params.set("q", term);
    if (searchCategory) params.set("category", searchCategory);
    const target = params.toString();
    router.push(target ? `/?${target}` : "/");
  };

  const query = searchParams.toString();
  const returnUrl = `${pathname}${query ? `?${query}` : ""}`;

  return (
    <header className="store-header">
      <div className="store-utility">
        <div className="container d-flex align-items-center gap-2 py-2">
          <Icon name="truck" size={14} />
          <span>Free shipping over $1,000</span>
        </div>
      </div>
      <div className="store-header-top">
        <div className="container d-flex flex-wrap align-items-center py-3 store-header-row">
          <Link className="store-logo" href="/">
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
            {session ? (
              <UserMenu username={session.username} />
            ) : (
              // A full page load: /sign-in is a server redirect to the login, not a page.
              <a
                className="store-header-link store-sign-in"
                href={`/sign-in?returnUrl=${encodeURIComponent(returnUrl)}`}
                aria-label="Sign In"
              >
                <span className="store-header-hint">Hello</span>
                <span>Sign in</span>
              </a>
            )}
            <Link className="store-header-link store-cart" href="/cart" aria-label="Cart">
              <span className="store-cart-icon">
                <Icon name="shopping-cart" size={26} strokeWidth={1.8} />
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
            <Link className="store-nav-link" href="/">
              All products
            </Link>
            {categories.map((category) => (
              <Link key={category} className="store-nav-link" href={`/?category=${encodeURIComponent(category)}`}>
                {category}
              </Link>
            ))}
          </div>
          {session && (
            <div className="store-nav-group store-nav-account">
              {session.canPlaceOrder && (
                <Link className="store-nav-link" href="/new-order">
                  Quick order
                </Link>
              )}
              <Link className="store-nav-link" href="/order-history">
                {session.isAdmin ? "All orders" : "My orders"}
              </Link>
              {session.isAdmin && (
                <Link className="store-nav-link" href="/admin-coupons">
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
