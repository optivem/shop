import * as http from 'http';
import type { AddressInfo } from 'net';
import { exportJWK, generateKeyPair, SignJWT, type KeyLike } from 'jose';

export const TEST_ISSUER = 'http://test-idp.local/realms/shop';
export const TEST_AUDIENCE = 'shop-backend';

/**
 * A minimal in-process identity provider for the component tests: it serves a real JWKS over HTTP
 * (so the backend exercises its real remote-key-set validation) and signs tokens the way Keycloak
 * would — realm roles under realm_access.roles, audience shop-backend.
 */
export class TestIdp {
  private readonly server: http.Server;
  private privateKey!: KeyLike;
  private jwks = '';
  jwkSetUri = '';

  constructor() {
    this.server = http.createServer((_req, res) => {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(this.jwks);
    });
  }

  async start(): Promise<void> {
    const { privateKey, publicKey } = await generateKeyPair('RS256');
    this.privateKey = privateKey;
    const jwk = await exportJWK(publicKey);
    this.jwks = JSON.stringify({
      keys: [{ ...jwk, kid: 'test-key', alg: 'RS256', use: 'sig' }],
    });
    await new Promise<void>((resolve) =>
      this.server.listen(0, '127.0.0.1', resolve),
    );
    const port = (this.server.address() as AddressInfo).port;
    this.jwkSetUri = `http://127.0.0.1:${port}/certs`;
  }

  async stop(): Promise<void> {
    await new Promise<void>((resolve, reject) =>
      this.server.close((err) => (err ? reject(err) : resolve())),
    );
  }

  async token(
    roles: string[],
    opts: { issuer?: string; audience?: string; expiresIn?: string } = {},
  ): Promise<string> {
    return new SignJWT({ realm_access: { roles } })
      .setProtectedHeader({ alg: 'RS256', kid: 'test-key' })
      .setSubject('test-user')
      .setIssuer(opts.issuer ?? TEST_ISSUER)
      .setAudience(opts.audience ?? TEST_AUDIENCE)
      .setIssuedAt()
      .setExpirationTime(opts.expiresIn ?? '1h')
      .sign(this.privateKey);
  }

  adminToken(): Promise<string> {
    return this.token(['ADMIN']);
  }

  customerToken(): Promise<string> {
    return this.token(['CUSTOMER']);
  }
}
