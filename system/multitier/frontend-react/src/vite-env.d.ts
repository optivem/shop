/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Local Keycloak the Vite dev server signs in against (run-local.sh dev); unset means http://localhost:8191. */
  readonly VITE_KEYCLOAK_URL?: string;
}
