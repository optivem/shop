import type { INestApplication } from '@nestjs/common';

// The API answers JSON; the only HTML it serves is Swagger UI, which needs inline script/style and data: images.
// Everything is same-origin (B-PRG 4.3, the twin of the Java and .NET backends' policy).
export const CONTENT_SECURITY_POLICY =
  "default-src 'self'; script-src 'self' 'unsafe-inline'; " +
  "style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'; " +
  "object-src 'none'; base-uri 'self'; frame-ancestors 'none'";

// HSTS goes on every response because TLS ends at the Cloud Run front and the app itself only sees http.
export const SECURITY_HEADERS: Readonly<Record<string, string>> = {
  'Content-Security-Policy': CONTENT_SECURITY_POLICY,
  'Referrer-Policy': 'strict-origin-when-cross-origin',
  'Strict-Transport-Security': 'max-age=31536000',
  'X-Content-Type-Options': 'nosniff',
  'X-Frame-Options': 'DENY',
};

interface HeaderSink {
  setHeader(name: string, value: string): unknown;
}

export function securityHeaders(
  _request: unknown,
  response: HeaderSink,
  next: () => void,
): void {
  for (const [name, value] of Object.entries(SECURITY_HEADERS)) {
    response.setHeader(name, value);
  }
  next();
}

/** Registered before every route, Swagger UI included; also drops Express's "X-Powered-By". */
export function useSecurityHeaders(app: INestApplication): void {
  const express = app.getHttpAdapter().getInstance() as {
    disable(setting: string): void;
  };
  express.disable('x-powered-by');
  app.use(securityHeaders);
}
