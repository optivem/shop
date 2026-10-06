import { generateKeyPair, SignJWT, type KeyLike } from 'jose';
import { JwtVerifier } from './jwt-verifier';

const config = {
  issuer: 'http://idp.test/realms/shop',
  jwkSetUri: 'http://idp.test/certs',
  audience: 'shop-backend',
};

describe('JwtVerifier', () => {
  let privateKey: KeyLike;
  let publicKey: KeyLike;
  let verifier: JwtVerifier;

  beforeAll(async () => {
    ({ privateKey, publicKey } = await generateKeyPair('RS256'));
    verifier = new JwtVerifier(config, () => Promise.resolve(publicKey));
  });

  const sign = (
    claims: Record<string, unknown>,
    opts: {
      issuer?: string;
      audience?: string;
      expiresIn?: string;
      key?: KeyLike;
    } = {},
  ) =>
    new SignJWT(claims)
      .setProtectedHeader({ alg: 'RS256' })
      .setSubject('user-1')
      .setIssuer(opts.issuer ?? config.issuer)
      .setAudience(opts.audience ?? config.audience)
      .setIssuedAt()
      .setExpirationTime(opts.expiresIn ?? '5m')
      .sign(opts.key ?? privateKey);

  it('accepts a valid token and maps realm roles', async () => {
    const token = await sign({
      realm_access: { roles: ['ADMIN', 'CUSTOMER'] },
      preferred_username: 'alice',
    });
    await expect(verifier.verify(token)).resolves.toEqual({
      subject: 'user-1',
      username: 'alice',
      roles: ['ADMIN', 'CUSTOMER'],
    });
  });

  it('maps a token without realm_access to no roles', async () => {
    const token = await sign({});
    await expect(verifier.verify(token)).resolves.toMatchObject({ roles: [] });
  });

  it('rejects a token signed with another key', async () => {
    const other = await generateKeyPair('RS256');
    const token = await sign({}, { key: other.privateKey });
    await expect(verifier.verify(token)).rejects.toThrow();
  });

  it('rejects a token from another issuer', async () => {
    const token = await sign({}, { issuer: 'http://evil.test/realms/shop' });
    await expect(verifier.verify(token)).rejects.toThrow();
  });

  it('rejects a token for another audience', async () => {
    const token = await sign({}, { audience: 'some-other-api' });
    await expect(verifier.verify(token)).rejects.toThrow();
  });

  it('rejects an expired token', async () => {
    const token = await sign({}, { expiresIn: '-1m' });
    await expect(verifier.verify(token)).rejects.toThrow();
  });

  it('rejects garbage', async () => {
    await expect(verifier.verify('garbage')).rejects.toThrow();
  });
});
