import { createContext } from 'react';

/** True when no Keycloak is configured: no login, no bearer token, and every role check passes. */
export const AuthDisabledContext = createContext(false);
