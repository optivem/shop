import type { Metadata } from "next";
import { cookies } from "next/headers";
import { StorefrontProviders, type SessionUser } from "@/components/StorefrontProviders";
import { ADMIN, CUSTOMER } from "@/lib/auth/policy";
import { SESSION_COOKIE, openSession } from "@/lib/auth/session";
import { CART_COOKIE, parseCart } from "@/lib/storefront/cart-cookie";
import { loadCatalog } from "@/lib/storefront/catalog-server";
import "./globals.css";

// Same-origin state-changing fetch() calls carry a custom header, which the server requires for
// cookie-authenticated API requests (a cross-site request cannot set it without a CORS preflight).
const CSRF_FETCH_SCRIPT = `(function () {
  var originalFetch = window.fetch;
  window.fetch = function (input, init) {
    init = init || {};
    var method = (init.method || 'GET').toUpperCase();
    if (method !== 'GET' && method !== 'HEAD') {
      init.headers = new Headers(init.headers || {});
      init.headers.set('X-Requested-With', 'fetch');
    }
    return originalFetch.call(this, input, init);
  };
})();`;

export const metadata: Metadata = {
  title: "MyShop",
  description: "MyShop application",
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const cookieStore = await cookies();
  const session = await openSession(cookieStore.get(SESSION_COOKIE)?.value);
  const user: SessionUser | null = session
    ? { username: session.name, isAdmin: session.roles.includes(ADMIN), canPlaceOrder: session.roles.includes(CUSTOMER) }
    : null;
  // The header shows the catalog's categories on every page; an unavailable ERP leaves them out.
  const catalog = await loadCatalog();
  const cart = parseCart(cookieStore.get(CART_COOKIE)?.value);

  return (
    <html lang="en">
      <head>
        <script dangerouslySetInnerHTML={{ __html: CSRF_FETCH_SCRIPT }} />
        <link
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet"
        />
      </head>
      <body>
        <StorefrontProviders session={user} products={catalog.products} catalogUnavailable={catalog.unavailable} cart={cart}>
          {children}
        </StorefrontProviders>
        <script
          src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"
          async
        />
      </body>
    </html>
  );
}
