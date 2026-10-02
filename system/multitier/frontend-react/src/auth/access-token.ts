// Hooks that let the API client attach the bearer token and react to 401 without depending on the auth library.

type TokenSource = () => Promise<string | undefined>;
type UnauthorizedHandler = () => void;

let tokenSource: TokenSource = () => Promise.resolve(undefined);
let unauthorizedHandler: UnauthorizedHandler = () => {
  // No handler registered (e.g. in tests).
};

export function setAccessTokenSource(source: TokenSource): void {
  tokenSource = source;
}

export function setUnauthorizedHandler(handler: UnauthorizedHandler): void {
  unauthorizedHandler = handler;
}

export function getAccessToken(): Promise<string | undefined> {
  return tokenSource();
}

export function notifyUnauthorized(): void {
  unauthorizedHandler();
}
