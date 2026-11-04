import {
  deleteExpiredIdempotencyKey,
  findIdempotencyKey,
  inTransaction,
  insertIdempotencyKey,
  isIdempotencyKeyConflict,
  type IdempotencyKeyRow,
  type Queryable,
} from './db';
import { expiryCutoff, isExpired } from './idempotency';

/** Who used a key, the key itself, and the fingerprint of the request it was used for. */
export interface IdempotencyScope {
  owner: string;
  key: string;
  fingerprint: string;
}

export type StoredAnswer =
  | { kind: 'replay'; status: number; body: string }
  | { kind: 'conflict' };

export type PlacementOutcome = { kind: 'placed' } | StoredAnswer;

function answerFrom(row: IdempotencyKeyRow, fingerprint: string): StoredAnswer {
  return row.request_hash === fingerprint
    ? { kind: 'replay', status: row.response_status, body: row.response_body }
    : { kind: 'conflict' };
}

/** The stored answer for a live key, or null when the key is unused or expired. */
export async function findStoredAnswer(scope: IdempotencyScope, now: Date = new Date()): Promise<StoredAnswer | null> {
  const row = await findIdempotencyKey(scope.owner, scope.key);
  return row && !isExpired(row.created_at, now) ? answerFrom(row, scope.fingerprint) : null;
}

/**
 * Runs the order's writes in one transaction. With a scope, the key row is claimed first (an expired
 * one is replaced), so a concurrent request with the same key fails on the unique index before it
 * touches the coupon; it then rolls back, placed nothing, and answers from the winner's row. Anything
 * the work throws rolls the key back too, so a failed request leaves the key free.
 */
export async function placeIdempotently(
  scope: IdempotencyScope | null,
  response: { status: number; body: string },
  work: (db: Queryable) => Promise<void>
): Promise<PlacementOutcome> {
  if (!scope) {
    await inTransaction(work);
    return { kind: 'placed' };
  }
  const now = new Date();
  try {
    await inTransaction(async (db) => {
      await deleteExpiredIdempotencyKey(scope.owner, scope.key, expiryCutoff(now), db);
      await insertIdempotencyKey(
        {
          owner: scope.owner,
          key: scope.key,
          requestHash: scope.fingerprint,
          responseStatus: response.status,
          responseBody: response.body,
          createdAt: now,
        },
        db
      );
      await work(db);
    });
    return { kind: 'placed' };
  } catch (error) {
    if (!isIdempotencyKeyConflict(error)) {
      throw error;
    }
    const winner = await findIdempotencyKey(scope.owner, scope.key);
    if (!winner) {
      throw error;
    }
    return answerFrom(winner, scope.fingerprint);
  }
}
