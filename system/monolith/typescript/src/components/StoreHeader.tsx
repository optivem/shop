"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { categoriesOf } from "@/lib/storefront/catalog-query";
import { useCart, useCatalog, useSession } from "./StorefrontProviders";

/** Shows the signed-in username (linking to My account) and a logout button. */
function UserMenu({ username }: Readonly<{ username: string }>) {
  return (
    <form method="post" action="/logout" className="d-flex align-items-center">
      <Link className="store-header-link me-3" href="/account" data-testid="username" title="My account">
        {username}
      </Link>
      <button type="submit" className="btn btn-outline-light btn-sm">
        Logout
      </button>
    </form>
  );
}

/** The header on every page (storefront-ux §3): logo, search, cart, sign-in state, categories and account links. */
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

  // Follow the address when it changes (back button, "Clear filters"), without an effect.
  if (shownQuery !== currentQuery) {
    setShownQuery(currentQuery);
    setSearch(currentQuery);
  }

  // A plain GET /?q= without JavaScript; with it, a client-side navigation.
  const onSearch = (event: FormEvent) => {
    event.preventDefault();
    const term = search.trim();
    router.push(term ? `/?q=${encodeURIComponent(term)}` : "/");
  };

  const query = searchParams.toString();
  const returnUrl = `${pathname}${query ? `?${query}` : ""}`;

  return (
    <header className="store-header">
      <div className="store-header-top">
        <div className="container d-flex flex-wrap align-items-center gap-3 py-2">
          <Link className="store-logo" href="/">
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
            <Link className="store-header-link" href="/cart" aria-label="Cart">
              <span aria-hidden="true">🛒</span> Cart
              <span aria-live="polite">
                {totalQuantity > 0 && (
                  <span className="badge rounded-pill bg-warning text-dark ms-1" aria-label="Cart Item Count">
                    {totalQuantity}
                  </span>
                )}
              </span>
            </Link>
            {session ? (
              <UserMenu username={session.username} />
            ) : (
              // A full page load: /sign-in is a server redirect to the login, not a page.
              <a className="store-header-link" href={`/sign-in?returnUrl=${encodeURIComponent(returnUrl)}`} aria-label="Sign In">
                Sign in
              </a>
            )}
          </div>
        </div>
      </div>
      <nav className="store-header-nav" aria-label="Store navigation">
        <div className="container d-flex flex-wrap align-items-center gap-3 py-1">
          <Link className="store-nav-link" href="/">
            All products
          </Link>
          {categoriesOf(products).map((category) => (
            <Link key={category} className="store-nav-link" href={`/?category=${encodeURIComponent(category)}`}>
              {category}
            </Link>
          ))}
          {session && (
            <div className="d-flex flex-wrap gap-3 ms-auto">
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
