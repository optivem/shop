import { SECURITY_HEADERS, securityHeaders } from './security-headers';

describe('securityHeaders', () => {
  it('sets every security header and passes the request on', () => {
    const headers = new Map<string, string>();
    const next = jest.fn();

    securityHeaders(
      {},
      { setHeader: (name: string, value: string) => headers.set(name, value) },
      next,
    );

    expect(Object.fromEntries(headers)).toEqual(SECURITY_HEADERS);
    expect(headers.get('X-Frame-Options')).toBe('DENY');
    expect(headers.get('Referrer-Policy')).toBe(
      'strict-origin-when-cross-origin',
    );
    expect(next).toHaveBeenCalledTimes(1);
  });
});
