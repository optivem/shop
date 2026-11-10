import { createContext } from 'react';

/**
 * False while the app is still finding out, silently, whether the browser already has a Keycloak session.
 * Until then the header shows neither "Sign In" nor the signed-in links, and protected routes wait before
 * redirecting. Defaults to true, so isolated component tests and the auth-disabled mode never wait.
 */
export const SessionCheckedContext = createContext(true);
