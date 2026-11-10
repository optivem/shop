// Where the browser goes back to after the Keycloak login (storefront-ux §6.4).

/** Only same-app paths are followed, so a crafted returnUrl can't send the visitor to another site. */
export function safeReturnUrl(value: unknown, fallback = '/'): string {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//') || value.startsWith('/\\')) {
    return fallback;
  }
  return value;
}

/** The login state carried through Keycloak: the page to come back to. */
export interface SigninState {
  returnUrl: string;
}

export function returnUrlFromState(state: unknown): string | undefined {
  if (typeof state !== 'object' || state === null) return undefined;
  const returnUrl = (state as Partial<SigninState>).returnUrl;
  return typeof returnUrl === 'string' ? safeReturnUrl(returnUrl) : undefined;
}

export function currentPath(): string {
  return `${globalThis.location.pathname}${globalThis.location.search}`;
}
