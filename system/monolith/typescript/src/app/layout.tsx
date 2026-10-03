import type { Metadata } from "next";
import Link from "next/link";
import { cookies } from "next/headers";
import { SESSION_COOKIE, openSession } from "@/lib/auth/session";
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
  const session = await openSession((await cookies()).get(SESSION_COOKIE)?.value);

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
        <nav className="navbar navbar-expand-lg navbar-dark bg-primary">
          <div className="container">
            <Link className="navbar-brand" href="/">
              MyShop
            </Link>
            {session && (
              <form method="post" action="/logout" className="d-flex align-items-center ms-auto">
                <span className="navbar-text me-3">{session.name}</span>
                <button type="submit" className="btn btn-outline-light btn-sm">
                  Logout
                </button>
              </form>
            )}
          </div>
        </nav>
        <div className="container mt-4">{children}</div>
        <script
          src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"
          async
        />
      </body>
    </html>
  );
}
