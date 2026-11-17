const purgeExpiredIdempotencyKeys = jest.fn();
jest.mock('@/lib/db', () => ({ purgeExpiredIdempotencyKeys }));
jest.mock('@/lib/logger', () => ({ logger: { info: jest.fn(), warn: jest.fn() } }));

import { purgeOnce } from '@/lib/idempotency-purge';

describe('purgeOnce', () => {
  it('deletes keys created 24 hours or more before now', async () => {
    purgeExpiredIdempotencyKeys.mockResolvedValue(3);

    await expect(purgeOnce(new Date('2026-01-02T00:00:00Z'))).resolves.toBe(3);

    expect(purgeExpiredIdempotencyKeys).toHaveBeenCalledWith(new Date('2026-01-01T00:00:00Z'));
  });

  it('reports nothing purged when the database call fails', async () => {
    purgeExpiredIdempotencyKeys.mockRejectedValue(new Error('down'));

    await expect(purgeOnce(new Date('2026-01-02T00:00:00Z'))).resolves.toBe(0);
  });
});
