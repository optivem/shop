// Same-origin only (B-PRG 4.3, the twin of the Java and .NET monoliths' policy). The layout carries inline scripts
// and style attributes, hence 'unsafe-inline'; everything the pages load (Bootstrap, the self-hosted font, images) is
// served from this app. form-action is left out on purpose: browsers apply it to the redirect to Keycloak that
// follows the logout form. `next dev` also needs 'unsafe-eval' (React Refresh); production builds do not.
const scriptSrc =
  process.env.NODE_ENV === "production" ? "'self' 'unsafe-inline'" : "'self' 'unsafe-inline' 'unsafe-eval'";

const contentSecurityPolicy = [
  "default-src 'self'",
  `script-src ${scriptSrc}`,
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data:",
  "font-src 'self' data:",
  "connect-src 'self'",
  "object-src 'none'",
  "base-uri 'self'",
  "frame-ancestors 'none'",
].join("; ");

// HSTS goes on every response because TLS ends at the Cloud Run front and the app itself only sees http.
const securityHeaders = [
  { key: "Content-Security-Policy", value: contentSecurityPolicy },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "Strict-Transport-Security", value: "max-age=31536000" },
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "X-Frame-Options", value: "DENY" },
];

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  // No "X-Powered-By: Next.js": it only tells a scanner what to aim at.
  poweredByHeader: false,
  headers() {
    return Promise.resolve([{ source: "/:path*", headers: securityHeaders }]);
  },
};

export default nextConfig;
